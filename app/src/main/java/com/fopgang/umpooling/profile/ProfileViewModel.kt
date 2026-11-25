package com.fopgang.umpooling.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fopgang.umpooling.data.EmergencyContact
import com.fopgang.umpooling.data.UserProfile
import com.fopgang.umpooling.data.UserRole
import com.fopgang.umpooling.data.repository.ProfileGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val profile: UserProfile? = null,
    val form: ProfileForm = ProfileForm(),
    val problems: Set<ProfileField> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val saveFailed: Boolean = false,
    val contact: EmergencyContact? = null,
    val contactInvalid: Boolean = false,
    val contactSaved: Boolean = false,
)

/** Loads, edits and saves the signed-in user's profile and emergency contact (FR1.3). */
class ProfileViewModel(
    private val gateway: ProfileGateway,
    private val scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val scope get() = scopeOverride ?: viewModelScope
    private val mutable = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutable.asStateFlow()

    init { load() }

    fun load() {
        mutable.update { it.copy(loading = true, loadFailed = false) }
        scope.launch {
            gateway.load()
                .onSuccess { p ->
                    mutable.update {
                        it.copy(loading = false, profile = p, form = p.toForm(), contact = p.emergencyContact)
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    mutable.update { it.copy(loading = false, loadFailed = true) }
                }
        }
    }

    fun edit(change: (ProfileForm) -> ProfileForm) {
        mutable.update {
            val form = change(it.form).tidied()
            // Re-check only after a failed attempt, so the form is not red before the user types.
            it.copy(form = form, saved = false, saveFailed = false,
                problems = if (it.problems.isEmpty()) emptySet() else form.problems())
        }
    }

    fun save() {
        val form = mutable.value.form.tidied()
        val problems = form.problems()
        if (problems.isNotEmpty()) {
            mutable.update { it.copy(problems = problems, saved = false) }
            return
        }
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, saved = false, saveFailed = false, problems = emptySet()) }
        scope.launch {
            gateway.save(form)
                .onSuccess { mutable.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    mutable.update { it.copy(saving = false, saveFailed = true) }
                }
        }
    }

    /** Pass blank phone to remove the contact. */
    fun saveContact(name: String, phone: String) {
        val clean = ProfileRules.normalizePhone(phone)
        if (clean.isNotEmpty() && !ProfileRules.isValidPhone(clean)) {
            mutable.update { it.copy(contactInvalid = true, contactSaved = false) }
            return
        }
        val contact = if (clean.isEmpty()) null else EmergencyContact(name.trim().take(ProfileRules.NAME_MAX), clean)
        mutable.update { it.copy(saving = true, contactInvalid = false, contactSaved = false, saveFailed = false) }
        scope.launch {
            gateway.saveEmergencyContact(contact)
                .onSuccess { mutable.update { it.copy(saving = false, contact = contact, contactSaved = true) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    mutable.update { it.copy(saving = false, saveFailed = true) }
                }
        }
    }

    fun clearMessages() {
        mutable.update { it.copy(saved = false, saveFailed = false, contactSaved = false, contactInvalid = false) }
    }
}

internal fun UserProfile.toForm() = ProfileForm(
    displayName = displayName.orEmpty(),
    faculty = faculty.orEmpty(),
    residentialArea = residentialArea.orEmpty(),
    residentialCollege = residentialCollege.orEmpty(),
    role = if (usersRole in UserRole.selectable) usersRole else UserRole.PASSENGERS,
    vehicleType = vehicleType.orEmpty(),
    vehiclePlate = vehiclePlate.orEmpty(),
    vehicleModel = vehicleModel.orEmpty(),
    fuelCategory = fuelCategory.orEmpty(),
)

/** Keeps vehicle choices consistent: a motorcycle is petrol only, a new type needs a valid fuel. */
internal fun ProfileForm.tidied(): ProfileForm {
    val fuels = ProfileRules.fuelCategories(vehicleType)
    return if (fuelCategory.isNotEmpty() && fuelCategory !in fuels) copy(fuelCategory = fuels.first()) else this
}

/** Used in demo mode so the screens work without a backend. Nothing is stored. */
class DemoProfileGateway : ProfileGateway {
    private var profile = UserProfile(usersId = "demo", usersEmail = "demo@siswa.um.edu.my", displayName = "Demo commuter")
    override suspend fun load() = Result.success(profile)
    override suspend fun save(form: ProfileForm): Result<Unit> {
        profile = profile.copy(displayName = form.displayName, faculty = form.faculty, usersRole = form.role)
        return Result.success(Unit)
    }
    override suspend fun saveEmergencyContact(contact: EmergencyContact?): Result<Unit> {
        profile = profile.copy(emergencyContact = contact)
        return Result.success(Unit)
    }
}
