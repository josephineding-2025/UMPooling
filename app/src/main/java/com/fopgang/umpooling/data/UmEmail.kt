package com.fopgang.umpooling.data

enum class UmAffiliation { STUDENT, STAFF }

/** FR1.1: only UM student and staff addresses may register. */
object UmEmail {
    private val allowedDomains = setOf("siswa.um.edu.my", "um.edu.my")

    /** Trims and lowercases, so "  A@SISWA.UM.EDU.MY " becomes "a@siswa.um.edu.my". */
    fun normalize(email: String): String = email.trim().lowercase()

    /** True only for one local part and an exactly matching UM domain. */
    fun isValid(email: String): Boolean {
        val normalized = normalize(email)
        val parts = normalized.split('@')
        if (parts.size != 2) return false
        val (local, domain) = parts
        return local.isNotEmpty() && local.none { it.isWhitespace() } && domain in allowedDomains
    }

    /** FR1.3 badge: siswa.um.edu.my is a student, um.edu.my is staff, anything else is null. */
    fun affiliation(email: String?): UmAffiliation? {
        if (email == null || !isValid(email)) return null
        return if (normalize(email).endsWith("@siswa.um.edu.my")) UmAffiliation.STUDENT else UmAffiliation.STAFF
    }
}
