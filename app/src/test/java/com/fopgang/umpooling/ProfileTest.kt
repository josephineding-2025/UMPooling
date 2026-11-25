package com.fopgang.umpooling

import com.fopgang.umpooling.data.EmergencyContact
import com.fopgang.umpooling.data.UserProfile
import com.fopgang.umpooling.data.UserRole
import com.fopgang.umpooling.data.repository.ProfileGateway
import com.fopgang.umpooling.data.repository.toUpdate
import com.fopgang.umpooling.profile.ProfileField
import com.fopgang.umpooling.profile.ProfileForm
import com.fopgang.umpooling.profile.ProfileRules
import com.fopgang.umpooling.profile.ProfileViewModel
import com.fopgang.umpooling.profile.initials
import com.fopgang.umpooling.profile.problems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    @After fun close() { scope.cancel() }

    private val driver = ProfileForm(
        displayName = "Aina", role = UserRole.DRIVERS, vehicleType = "car",
        vehiclePlate = "vab 1234", fuelCategory = "petrol",
    )

    // ---- rules ----

    @Test fun platesAreNormalizedAndChecked() {
        assertEquals("VAB1234", ProfileRules.normalizePlate(" vab-1234 "))
        listOf("VAB1234", "W1234A", "pkt 99", "A1").forEach { assertTrue(it, ProfileRules.isValidPlate(it)) }
        listOf("", "1234", "ABCD1234", "VAB", "VAB12345", "V@B 12").forEach { assertFalse(it, ProfileRules.isValidPlate(it)) }
    }

    @Test fun phonesKeepOnlyDigitsAndALeadingPlus() {
        assertEquals("+60123456789", ProfileRules.normalizePhone(" +60 12-345 6789 "))
        assertEquals("0123456789", ProfileRules.normalizePhone("012 345 6789"))
        assertTrue(ProfileRules.isValidPhone("+60123456789"))
        assertFalse(ProfileRules.isValidPhone("12345"))
        assertFalse(ProfileRules.isValidPhone("1234567890123456"))
    }

    @Test fun motorcycleIsPetrolOnlyAndCarsHaveFourFuels() {
        assertEquals(listOf("petrol"), ProfileRules.fuelCategories("motorcycle"))
        assertEquals(listOf("petrol", "diesel", "hybrid", "ev"), ProfileRules.fuelCategories("car"))
    }

    @Test fun passengersNeedOnlyAName() {
        assertTrue(ProfileForm(displayName = "Aina").problems().isEmpty())
        assertEquals(setOf(ProfileField.NAME), ProfileForm(displayName = "  ").problems())
    }

    @Test fun driversNeedVehicleTypePlateAndFuel() {
        assertTrue(driver.problems().isEmpty())
        assertEquals(
            setOf(ProfileField.VEHICLE_TYPE, ProfileField.PLATE, ProfileField.FUEL),
            driver.copy(vehicleType = "", vehiclePlate = "", fuelCategory = "").problems(),
        )
        // A motorcycle cannot be diesel.
        assertEquals(setOf(ProfileField.FUEL), driver.copy(vehicleType = "motorcycle", fuelCategory = "diesel").problems())
        assertEquals(
            setOf(ProfileField.VEHICLE_TYPE, ProfileField.PLATE, ProfileField.FUEL),
            driver.copy(role = UserRole.BOTH, vehicleType = "", vehiclePlate = "", fuelCategory = "").problems(),
        )
    }

    @Test fun initialsHandleMissingAndMultiWordNames() {
        assertEquals("AS", initials("Aina Sofea Binti Ali"))
        assertEquals("A", initials("aina"))
        assertEquals("UM", initials(null))
        assertEquals("UM", initials("   "))
    }

    // ---- what is sent to the database ----

    @Test fun passengerUpdateClearsVehicleColumns() {
        val json = ProfileForm(displayName = " Aina ", faculty = "FSKTM", vehiclePlate = "OLD1", vehicleType = "car").toUpdate()
        assertEquals("Aina", json.getValue("display_name").jsonPrimitive.content)
        assertEquals("FSKTM", json.getValue("faculty").jsonPrimitive.content)
        assertEquals(JsonNull, json.getValue("residential_area"))
        listOf("vehicle_type", "vehiclePlate", "vehicleModel", "fuel_category").forEach { assertEquals(it, JsonNull, json.getValue(it)) }
    }

    @Test fun driverUpdateSendsTheNormalizedPlateAndNeverTheRole() {
        val json = driver.toUpdate()
        assertEquals("VAB1234", json.getValue("vehiclePlate").jsonPrimitive.content)
        assertEquals("car", json.getValue("vehicle_type").jsonPrimitive.content)
        assertFalse(json.containsKey("usersRole"))
    }

    // ---- view model ----

    private class FakeProfiles(var profile: UserProfile = UserProfile("u1", "a@siswa.um.edu.my")) : ProfileGateway {
        var loadResult: Result<UserProfile>? = null
        var saveResult = Result.success(Unit)
        var saved: ProfileForm? = null
        var contactSaved: EmergencyContact? = EmergencyContact(phone = "untouched")
        var loads = 0
        override suspend fun load(): Result<UserProfile> { loads++; return loadResult ?: Result.success(profile) }
        override suspend fun save(form: ProfileForm): Result<Unit> { saved = form; return saveResult }
        override suspend fun saveEmergencyContact(contact: EmergencyContact?): Result<Unit> {
            contactSaved = contact
            return saveResult
        }
    }

    private fun model(gateway: FakeProfiles) = ProfileViewModel(gateway, scope)

    @Test fun loadsTheProfileIntoTheForm() {
        val gateway = FakeProfiles(UserProfile(
            "u1", "a@siswa.um.edu.my", usersRole = UserRole.BOTH, displayName = "Aina", faculty = "FSKTM",
            vehiclePlate = "VAB1234", vehicleType = "car", fuelCategory = "ev",
            emergencyContact = EmergencyContact("Mum", "+60123456789"),
        ))
        val state = model(gateway).state.value
        assertFalse(state.loading)
        assertEquals("Aina", state.form.displayName)
        assertEquals(UserRole.BOTH, state.form.role)
        assertEquals("ev", state.form.fuelCategory)
        assertEquals("Mum", state.contact?.name)
    }

    @Test fun staffRoleShowsAsPassengerInTheFormSoItIsNeverSent() {
        val state = model(FakeProfiles(UserProfile("u1", "a@um.edu.my", usersRole = "staffAnalyst"))).state.value
        assertEquals(UserRole.PASSENGERS, state.form.role)
    }

    @Test fun failedLoadCanBeRetried() {
        val gateway = FakeProfiles()
        gateway.loadResult = Result.failure(RuntimeException("offline"))
        val vm = model(gateway)
        assertTrue(vm.state.value.loadFailed)
        gateway.loadResult = null
        vm.load()
        assertFalse(vm.state.value.loadFailed)
        assertEquals(2, gateway.loads)
    }

    @Test fun invalidFormIsNotSentAndShowsTheBadFields() {
        val gateway = FakeProfiles()
        val vm = model(gateway)
        vm.edit { it.copy(displayName = "", role = UserRole.DRIVERS) }
        vm.save()
        assertNull(gateway.saved)
        assertTrue(ProfileField.NAME in vm.state.value.problems)
        assertTrue(ProfileField.PLATE in vm.state.value.problems)
        // Fixing a field clears its error as the user types.
        vm.edit { it.copy(displayName = "Aina") }
        assertFalse(ProfileField.NAME in vm.state.value.problems)
    }

    @Test fun validFormIsSavedOnce() {
        val gateway = FakeProfiles()
        val vm = model(gateway)
        vm.edit { driver }
        vm.save()
        assertEquals(driver, gateway.saved)
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.saving)
        assertTrue(vm.state.value.problems.isEmpty())
    }

    @Test fun failedSaveKeepsTheFormAndReportsIt() {
        val gateway = FakeProfiles()
        gateway.saveResult = Result.failure(RuntimeException("offline"))
        val vm = model(gateway)
        vm.edit { it.copy(displayName = "Aina") }
        vm.save()
        assertTrue(vm.state.value.saveFailed)
        assertFalse(vm.state.value.saved)
        assertEquals("Aina", vm.state.value.form.displayName)
    }

    @Test fun switchingToMotorcycleResetsAnInvalidFuel() {
        val vm = model(FakeProfiles())
        vm.edit { driver.copy(fuelCategory = "diesel") }
        vm.edit { it.copy(vehicleType = "motorcycle") }
        assertEquals("petrol", vm.state.value.form.fuelCategory)
    }

    @Test fun contactIsNormalizedBeforeSaving() {
        val gateway = FakeProfiles()
        val vm = model(gateway)
        vm.saveContact(" Mum ", "+60 12-345 6789")
        assertEquals(EmergencyContact("Mum", "+60123456789"), gateway.contactSaved)
        assertTrue(vm.state.value.contactSaved)
        assertEquals("Mum", vm.state.value.contact?.name)
    }

    @Test fun badContactNumberIsRejectedWithoutAnyCall() {
        val gateway = FakeProfiles()
        val vm = model(gateway)
        vm.saveContact("Mum", "12345")
        assertTrue(vm.state.value.contactInvalid)
        assertEquals(EmergencyContact(phone = "untouched"), gateway.contactSaved)
    }

    @Test fun blankNumberRemovesTheContact() {
        val gateway = FakeProfiles(UserProfile("u1", "a@siswa.um.edu.my", emergencyContact = EmergencyContact("Mum", "+60123456789")))
        val vm = model(gateway)
        vm.saveContact("", "")
        assertNull(gateway.contactSaved)
        assertNull(vm.state.value.contact)
        assertTrue(vm.state.value.contactSaved)
    }
}
