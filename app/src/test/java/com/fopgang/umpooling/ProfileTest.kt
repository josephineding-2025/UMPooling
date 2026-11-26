package com.fopgang.umpooling

import com.fopgang.umpooling.data.EmergencyContact
import com.fopgang.umpooling.data.UserProfile
import com.fopgang.umpooling.data.UserRole
import com.fopgang.umpooling.ai.GreenCommuterTier
import com.fopgang.umpooling.data.repository.PersonalImpact
import com.fopgang.umpooling.data.repository.ProfileGateway
import com.fopgang.umpooling.profile.ImpactSummary
import com.fopgang.umpooling.data.repository.toUpdate
import com.fopgang.umpooling.profile.ProfileField
import com.fopgang.umpooling.profile.ProfileForm
import com.fopgang.umpooling.profile.ProfileRules
import com.fopgang.umpooling.profile.ProfileViewModel
import com.fopgang.umpooling.profile.initials
import com.fopgang.umpooling.profile.verifiedBadge
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
        var impactResult: Result<PersonalImpact?> = Result.success(null)
        override suspend fun impact() = impactResult
        override suspend fun load(): Result<UserProfile> { loads++; return loadResult ?: Result.success(profile) }
        override suspend fun save(form: ProfileForm): Result<Unit> { saved = form; return saveResult }
        override suspend fun saveEmergencyContact(contact: EmergencyContact?): Result<Unit> {
            contactSaved = contact
            return saveResult
        }
    }

    private fun model(gateway: FakeProfiles) = ProfileViewModel(gateway, scope)

    // ---- verification badge ----

    @Test fun badgeFollowsTheVerifiedUmEmailDomain() {
        assertEquals(R.string.verified_student, verifiedBadge(" A@SISWA.UM.EDU.MY "))
        assertEquals(R.string.verified_staff, verifiedBadge("lecturer@um.edu.my"))
        // Lookalike domains and a profile that has not loaded yet get no badge.
        assertNull(verifiedBadge("a@siswa.um.edu.my.evil.com"))
        assertNull(verifiedBadge("a@notum.edu.my"))
        assertNull(verifiedBadge(null))
    }

    // ---- tier and CO2 summary ----

    private fun impact(trips: Int, co2: Double, level: Int = 0) =
        PersonalImpact(userId = "u1", totalTrips = trips, co2Kg = co2, tierLevel = level)

    @Test fun newUserHasNoBadgeAndNeedsOneTripForSeedling() {
        val s = ImpactSummary.from(null, null)
        assertEquals(GreenCommuterTier.NO_BADGE, s.tier)
        assertEquals(GreenCommuterTier.SEEDLING, s.next)
        assertEquals(1, s.tripsToNext)
        assertEquals(0.5, s.co2KgToNext, 1e-9)
        assertEquals("0", s.tripsText)
        assertEquals("0.0", s.co2Text)
        assertEquals("-", s.ratingText)
    }

    @Test fun tierNeedsBothTripsAndCo2() {
        // 5 trips but only 4 kg: still Seedling, 1 kg short of Sprout.
        val s = ImpactSummary.from(impact(5, 4.0), 4.86)
        assertEquals(GreenCommuterTier.SEEDLING, s.tier)
        assertEquals(GreenCommuterTier.SPROUT, s.next)
        assertEquals(0, s.tripsToNext)
        assertEquals(1.0, s.co2KgToNext, 1e-9)
        assertEquals("4.9", s.ratingText)
    }

    @Test fun storedTierIsNeverShownLowerThanTheServerKeptIt() {
        // The server never lowers a tier; show Sapling even if totals were corrected down.
        val s = ImpactSummary.from(impact(10, 8.0, level = 3), null)
        assertEquals(GreenCommuterTier.SAPLING, s.tier)
        assertEquals(GreenCommuterTier.TREE, s.next)
    }

    @Test fun topTierHasNoNextStep() {
        val s = ImpactSummary.from(impact(90, 150.0), null)
        assertEquals(GreenCommuterTier.FOREST_GUARDIAN, s.tier)
        assertNull(s.next)
        assertEquals(0, s.tripsToNext)
    }

    @Test fun badServerNumbersAreClampedAndUnratedShowsADash() {
        val s = ImpactSummary.from(impact(-3, -1.0), 0.0)
        assertEquals(0, s.trips)
        assertEquals(0.0, s.co2Kg, 0.0)
        assertEquals("-", s.ratingText)
    }

    @Test fun impactAndRatingReachTheProfileState() {
        val gateway = FakeProfiles(UserProfile("u1", "a@siswa.um.edu.my", trustRating = 4.5))
        gateway.impactResult = Result.success(impact(6, 5.5))
        val state = model(gateway).state.value
        assertEquals(GreenCommuterTier.SPROUT, state.impact.tier)
        assertEquals("4.5", state.impact.ratingText)
        assertFalse(state.impactFailed)
    }

    @Test fun failedImpactRefreshKeepsTheLastTotals() {
        val gateway = FakeProfiles()
        gateway.impactResult = Result.success(impact(6, 5.5))
        val vm = model(gateway)
        gateway.impactResult = Result.failure(RuntimeException("offline"))
        vm.refreshImpact()
        assertTrue(vm.state.value.impactFailed)
        assertEquals(6, vm.state.value.impact.trips)
        // A later trip shows up on the next refresh.
        gateway.impactResult = Result.success(impact(7, 6.7))
        vm.refreshImpact()
        assertEquals(7, vm.state.value.impact.trips)
        assertFalse(vm.state.value.impactFailed)
    }

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
