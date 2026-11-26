package com.fopgang.umpooling.profile

import com.fopgang.umpooling.data.UserRole

/** Allowed values and checks for the profile form. Pure Kotlin so they are unit tested. */
object ProfileRules {
    const val NAME_MAX = 60
    const val TEXT_MAX = 60
    const val MODEL_MAX = 40

    val vehicleTypes = listOf("car", "motorcycle")

    /** Fuel categories from docs/emission-factor.md. A motorcycle is petrol only. */
    fun fuelCategories(vehicleType: String?): List<String> =
        if (vehicleType == "motorcycle") listOf("petrol") else listOf("petrol", "diesel", "hybrid", "ev")

    fun needsVehicle(role: String): Boolean = role == UserRole.DRIVERS || role == UserRole.BOTH

    /** Upper case, no spaces or dashes. Malaysian plates look like VAB1234, W1234A or PKT99. */
    fun normalizePlate(input: String): String =
        input.filter { !it.isWhitespace() && it != '-' }.uppercase()

    fun isValidPlate(input: String): Boolean =
        Regex("^[A-Z]{1,3}[0-9]{1,4}[A-Z]?$").matches(normalizePlate(input))

    /** Keeps a leading plus and digits only. */
    fun normalizePhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (input.trim().startsWith("+")) "+$digits" else digits
    }

    fun isValidPhone(input: String): Boolean = normalizePhone(input).count { it.isDigit() } in 8..15
}

/** What the form edits. Blank text means "not set". */
data class ProfileForm(
    val displayName: String = "",
    val faculty: String = "",
    val residentialArea: String = "",
    val residentialCollege: String = "",
    val role: String = UserRole.PASSENGERS,
    val vehicleType: String = "",
    val vehiclePlate: String = "",
    val vehicleModel: String = "",
    val fuelCategory: String = "",
)

enum class ProfileField { NAME, FACULTY, AREA, COLLEGE, VEHICLE_TYPE, PLATE, MODEL, FUEL }

/** Returns the fields that fail. Vehicle details are only required for drivers. */
fun ProfileForm.problems(): Set<ProfileField> = buildSet {
    val name = displayName.trim()
    if (name.isEmpty() || name.length > ProfileRules.NAME_MAX) add(ProfileField.NAME)
    if (faculty.trim().length > ProfileRules.TEXT_MAX) add(ProfileField.FACULTY)
    if (residentialArea.trim().length > ProfileRules.TEXT_MAX) add(ProfileField.AREA)
    if (residentialCollege.trim().length > ProfileRules.TEXT_MAX) add(ProfileField.COLLEGE)
    if (ProfileRules.needsVehicle(role)) {
        if (vehicleType !in ProfileRules.vehicleTypes) add(ProfileField.VEHICLE_TYPE)
        if (!ProfileRules.isValidPlate(vehiclePlate)) add(ProfileField.PLATE)
        if (vehicleModel.trim().length > ProfileRules.MODEL_MAX) add(ProfileField.MODEL)
        if (fuelCategory !in ProfileRules.fuelCategories(vehicleType)) add(ProfileField.FUEL)
    }
}
