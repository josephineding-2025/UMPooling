package com.fopgang.umpooling.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.booking.RideAction
import com.fopgang.umpooling.booking.RideHeader
import com.fopgang.umpooling.booking.RideMuted
import com.fopgang.umpooling.booking.RidePage
import com.fopgang.umpooling.data.UserRole

/** FR1.3: personal details, role and vehicle. Vehicle fields appear for drivers only. */
@Composable
fun ProfileEditScreen(
    state: ProfileUiState,
    back: () -> Unit,
    onEdit: ((ProfileForm) -> ProfileForm) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    RidePage {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RideHeader(stringResource(R.string.profile_edit_title), back)
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(top = 24.dp))
                state.loadFailed -> {
                    Text(stringResource(R.string.profile_load_failed), color = MaterialTheme.colorScheme.error)
                    TextButton(onRetry) { Text(stringResource(R.string.profile_retry)) }
                }
                else -> ProfileFormBody(state, onEdit, onSave)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileFormBody(state: ProfileUiState, onEdit: ((ProfileForm) -> ProfileForm) -> Unit, onSave: () -> Unit) {
    val form = state.form
    val enabled = !state.saving
    fun bad(field: ProfileField) = field in state.problems

    Text(state.profile?.usersEmail.orEmpty(), color = RideMuted, fontSize = 13.sp)

    ProfileText(R.string.profile_name_label, form.displayName, bad(ProfileField.NAME), enabled, capitalize = true) {
        onEdit { f -> f.copy(displayName = it.take(ProfileRules.NAME_MAX)) }
    }
    ProfileText(R.string.profile_faculty, form.faculty, bad(ProfileField.FACULTY), enabled, capitalize = true) {
        onEdit { f -> f.copy(faculty = it.take(ProfileRules.TEXT_MAX)) }
    }
    ProfileText(R.string.profile_area, form.residentialArea, bad(ProfileField.AREA), enabled, capitalize = true) {
        onEdit { f -> f.copy(residentialArea = it.take(ProfileRules.TEXT_MAX)) }
    }
    ProfileText(R.string.profile_college, form.residentialCollege, bad(ProfileField.COLLEGE), enabled, capitalize = true) {
        onEdit { f -> f.copy(residentialCollege = it.take(ProfileRules.TEXT_MAX)) }
    }

    SectionLabel(R.string.profile_role)
    Choices(
        options = listOf(
            UserRole.PASSENGERS to R.string.role_passengers,
            UserRole.DRIVERS to R.string.role_drivers,
            UserRole.BOTH to R.string.role_both,
        ),
        selected = form.role, enabled = enabled,
    ) { role -> onEdit { f -> f.copy(role = role) } }

    if (ProfileRules.needsVehicle(form.role)) {
        SectionLabel(R.string.profile_vehicle_section)
        Choices(
            options = listOf("car" to R.string.vehicle_car, "motorcycle" to R.string.vehicle_motorcycle),
            selected = form.vehicleType, enabled = enabled, error = bad(ProfileField.VEHICLE_TYPE),
        ) { type -> onEdit { f -> f.copy(vehicleType = type) } }
        ProfileText(R.string.vehicle_plate, form.vehiclePlate, bad(ProfileField.PLATE), enabled, capitalize = true,
            supporting = if (bad(ProfileField.PLATE)) R.string.vehicle_plate_error else null) {
            onEdit { f -> f.copy(vehiclePlate = it.take(12)) }
        }
        ProfileText(R.string.vehicle_model, form.vehicleModel, bad(ProfileField.MODEL), enabled, capitalize = true) {
            onEdit { f -> f.copy(vehicleModel = it.take(ProfileRules.MODEL_MAX)) }
        }
        SectionLabel(R.string.fuel_label)
        Choices(
            options = ProfileRules.fuelCategories(form.vehicleType).map { it to fuelLabel(it) },
            selected = form.fuelCategory, enabled = enabled, error = bad(ProfileField.FUEL),
        ) { fuel -> onEdit { f -> f.copy(fuelCategory = fuel) } }
    }

    when {
        state.problems.isNotEmpty() -> Message(R.string.profile_check_fields, error = true)
        state.saveFailed -> Message(R.string.profile_save_failed, error = true)
        state.saved -> Message(R.string.profile_saved, error = false)
    }
    RideAction(stringResource(R.string.profile_save), enabled = enabled, action = onSave)
}

private fun fuelLabel(fuel: String): Int = when (fuel) {
    "diesel" -> R.string.fuel_diesel
    "hybrid" -> R.string.fuel_hybrid
    "ev" -> R.string.fuel_ev
    else -> R.string.fuel_petrol
}

@Composable
private fun SectionLabel(label: Int) {
    Text(stringResource(label), Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

@Composable
private fun Message(text: Int, error: Boolean) {
    Text(stringResource(text), color = if (error) MaterialTheme.colorScheme.error else RideMuted, fontWeight = FontWeight.Medium)
}

@Composable
private fun ProfileText(
    label: Int,
    value: String,
    error: Boolean,
    enabled: Boolean,
    capitalize: Boolean = false,
    supporting: Int? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, isError = error,
        label = { Text(stringResource(label)) },
        supportingText = supporting?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Choices(
    options: List<Pair<String, Int>>,
    selected: String,
    enabled: Boolean,
    error: Boolean = false,
    onSelect: (String) -> Unit,
) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected, onClick = { onSelect(value) }, enabled = enabled,
                label = { Text(stringResource(label), color = if (error && value != selected) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified) },
            )
        }
    }
}
