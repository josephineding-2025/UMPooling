package com.fopgang.umpooling.data

/**
 * Curated list of verified UM campus and off-campus commuter survey locations (v3.1.0).
 * Generated from docs/locations.json for instant UI selection and GPS coordinate lookup.
 */
data class CampusLocation(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val pickupName: String = name,
    val pickupLat: Double? = lat,
    val pickupLng: Double? = lng
) {
    val ewkt: String? get() = if (lng != null && lat != null) GeoPoint.ewkt(lng, lat) else null
    val pickupEwkt: String? get() = if (pickupLng != null && pickupLat != null) GeoPoint.ewkt(pickupLng, pickupLat) else null
}

object CampusLocationProvider {
    val allLocations: List<CampusLocation> = listOf(
        CampusLocation("fsktm", "Faculty of Computer Science and Information Technology", "Faculties, Academies & Academic Institutes", 3.12841, 101.65049, "Dataran FSKTM / Main Foyer Drop-off", 3.12825, 101.6508),
        CampusLocation("engineering", "Faculty of Engineering", "Faculties, Academies & Academic Institutes", 3.1175, 101.6533, "Faculty of Engineering", 3.1175, 101.6533),
        CampusLocation("science", "Faculty of Science", "Faculties, Academies & Academic Institutes", 3.1222, 101.653, "Faculty of Science", 3.1222, 101.653),
        CampusLocation("medicine", "Faculty of Medicine", "Faculties, Academies & Academic Institutes", 3.1135, 101.658, "Faculty of Medicine", 3.1135, 101.658),
        CampusLocation("dentistry", "Faculty of Dentistry", "Faculties, Academies & Academic Institutes", 3.114, 101.657, "Faculty of Dentistry", 3.114, 101.657),
        CampusLocation("pharmacy", "Faculty of Pharmacy", "Faculties, Academies & Academic Institutes", 3.1154, 101.6525, "Faculty of Pharmacy", 3.1154, 101.6525),
        CampusLocation("fbec", "Faculty of Business and Economics", "Faculties, Academies & Academic Institutes", 3.123, 101.6475, "Faculty of Business and Economics", 3.123, 101.6475),
        CampusLocation("law", "Faculty of Law", "Faculties, Academies & Academic Institutes", 3.1218, 101.646, "Faculty of Law", 3.1218, 101.646),
        CampusLocation("arts_social_sciences", "Faculty of Arts and Social Sciences", "Faculties, Academies & Academic Institutes", 3.1215, 101.6488, "Faculty of Arts and Social Sciences", 3.1215, 101.6488),
        CampusLocation("languages_linguistics", "Faculty of Languages and Linguistics", "Faculties, Academies & Academic Institutes", 3.1205, 101.6465, "Faculty of Languages and Linguistics", 3.1205, 101.6465),
        CampusLocation("built_environment", "Faculty of Built Environment", "Faculties, Academies & Academic Institutes", 3.1195, 101.648, "Faculty of Built Environment", 3.1195, 101.648),
        CampusLocation("education", "Faculty of Education", "Faculties, Academies & Academic Institutes", 3.1248, 101.6548, "Faculty of Education", 3.1248, 101.6548),
        CampusLocation("creative_arts", "Faculty of Creative Arts", "Faculties, Academies & Academic Institutes", 3.1212, 101.6568, "Faculty of Creative Arts", 3.1212, 101.6568),
        CampusLocation("islamic_studies", "Academy of Islamic Studies", "Faculties, Academies & Academic Institutes", 3.1253, 101.6467, "Academy of Islamic Studies", 3.1253, 101.6467),
        CampusLocation("malay_studies", "Academy of Malay Studies", "Faculties, Academies & Academic Institutes", 3.1248, 101.6524, "Academy of Malay Studies", 3.1248, 101.6524),
        CampusLocation("ips", "Institute of Postgraduate Studies", "Faculties, Academies & Academic Institutes", 3.1235, 101.655, "Institute of Postgraduate Studies", 3.1235, 101.655),
        CampusLocation("aei", "Asia-Europe Institute", "Faculties, Academies & Academic Institutes", 3.124, 101.6575, "Asia-Europe Institute", 3.124, 101.6575),
        CampusLocation("pasum", "Pusat Asasi Sains Universiti Malaya", "Faculties, Academies & Academic Institutes", 3.1229, 101.6591, "Pusat Asasi Sains Universiti Malaya", 3.1229, 101.6591),
        CampusLocation("kk1", "Kolej Kediaman Pertama (ASTAR / Tuanku Abdul Rahman)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.123, 101.6505, "Kolej Kediaman Pertama (ASTAR / Tuanku Abdul Rahman)", 3.123, 101.6505),
        CampusLocation("kk2", "Kolej Kediaman Kedua (Tuanku Bahiyah)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1163, 101.6572, "Kolej Kediaman Kedua (Tuanku Bahiyah)", 3.1163, 101.6572),
        CampusLocation("kk3", "Kolej Kediaman Ketiga (Tunku Kurshiah)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1246, 101.651, "Kolej Kediaman Ketiga (Tunku Kurshiah)", 3.1246, 101.651),
        CampusLocation("kk4", "Kolej Kediaman Keempat (Bestari)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1255, 101.65, "Kolej Kediaman Keempat (Bestari)", 3.1255, 101.65),
        CampusLocation("kk5", "Kolej Kediaman Kelima (Dayasari)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1286, 101.6605, "Kolej Kediaman Kelima (Dayasari)", 3.1286, 101.6605),
        CampusLocation("kk6", "Kolej Kediaman Keenam (Ibnu Sina)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1145, 101.6543, "Kolej Kediaman Keenam (Ibnu Sina)", 3.1145, 101.6543),
        CampusLocation("kk7", "Kolej Kediaman Ketujuh (Za'ba)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1253, 101.6508, "Kolej Kediaman Ketujuh (Za'ba)", 3.1253, 101.6508),
        CampusLocation("kk8", "Kolej Kediaman Kelapan (Kinabalu)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1299, 101.6493, "Kolej Kediaman Kelapan (Kinabalu)", 3.1299, 101.6493),
        CampusLocation("kk9", "Kolej Kediaman Kesembilan (Tun Syed Zahiruddin)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1209, 101.6457, "Kolej Kediaman Kesembilan (Tun Syed Zahiruddin)", 3.1209, 101.6457),
        CampusLocation("kk10", "Kolej Kediaman Kesepuluh (Tun Ahmad Zaidi)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1308, 101.6503, "Kolej Kediaman Kesepuluh (Tun Ahmad Zaidi)", 3.1308, 101.6503),
        CampusLocation("kk11", "Kolej Kediaman Kesebelas (Ungku Aziz)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1295, 101.6605, "Kolej Kediaman Kesebelas (Ungku Aziz)", 3.1295, 101.6605),
        CampusLocation("kk12", "Kolej Kediaman Kedua Belas (Raja Dr. Nazrin Shah)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1249, 101.6608, "Kolej Kediaman Kedua Belas (Raja Dr. Nazrin Shah)", 3.1249, 101.6608),
        CampusLocation("kk13", "Kolej Kediaman Ketiga Belas (KK13 - Asrama Sewaan Luar Kampus)", "Residential Colleges (Kolej Kediaman KK1 - KK13)", 3.1204, 101.6403, "Kolej Kediaman Ketiga Belas (KK13 - Asrama Sewaan Luar Kampus)", 3.1204, 101.6403),
        CampusLocation("main_library", "Main Library (Perpustakaan Utama)", "Central Facilities, Landmarks & Health Services", 3.12, 101.654, "Main Library (Perpustakaan Utama)", 3.12, 101.654),
        CampusLocation("zaba_library", "Za'ba Memorial Library", "Central Facilities, Landmarks & Health Services", 3.121, 101.6535, "Za'ba Memorial Library", 3.121, 101.6535),
        CampusLocation("dtc", "Dewan Tunku Canselor (DTC)", "Central Facilities, Landmarks & Health Services", 3.1208, 101.6555, "Dewan Tunku Canselor (DTC)", 3.1208, 101.6555),
        CampusLocation("exam_building", "Bangunan Peperiksaan (Examination Building)", "Central Facilities, Landmarks & Health Services", 3.1228, 101.6542, "Bangunan Peperiksaan (Examination Building)", 3.1228, 101.6542),
        CampusLocation("kps", "Kompleks Perdana Siswa (KPS)", "Central Facilities, Landmarks & Health Services", 3.1206, 101.6555, "Kompleks Perdana Siswa (KPS)", 3.1206, 101.6555),
        CampusLocation("um_arena", "UM Arena (Stadium Varsiti & Track)", "Central Facilities, Landmarks & Health Services", 3.131, 101.659, "Stadium Varsiti Main Entrance / Gate B", 3.1308, 101.6588),
        CampusLocation("um_sports_complex", "Pusat Sukan UM (Indoor Complex, Gymnasium & Olympic Pool)", "Central Facilities, Landmarks & Health Services", 3.1295, 101.6595, "Pusat Sukan Lobby & Pool Foyer Drop-off", 3.1292, 101.6592),
        CampusLocation("masjid_ar_rahman", "Masjid Ar-Rahman Universiti Malaya", "Central Facilities, Landmarks & Health Services", 3.1186, 101.6627, "Masjid Ar-Rahman Universiti Malaya", 3.1186, 101.6627),
        CampusLocation("ummc", "Pusat Perubatan Universiti Malaya (PPUM / UMMC)", "Central Facilities, Landmarks & Health Services", 3.1135, 101.652, "Pusat Perubatan Universiti Malaya (PPUM / UMMC)", 3.1135, 101.652),
        CampusLocation("umsc", "UM Specialist Centre (UMSC)", "Central Facilities, Landmarks & Health Services", 3.1142, 101.6515, "UM Specialist Centre (UMSC)", 3.1142, 101.6515),
        CampusLocation("wisma_rd", "Wisma R&D Universiti Malaya", "Central Facilities, Landmarks & Health Services", 3.1165, 101.665, "Wisma R&D Universiti Malaya", 3.1165, 101.665),
        CampusLocation("kl_gate", "KL Gate (Pintu Gerbang Utama Lembah Pantai)", "Campus Gates & Vehicular Access Points", 3.1159, 101.6627, "KL Gate (Pintu Gerbang Utama Lembah Pantai)", 3.1159, 101.6627),
        CampusLocation("pj_gate", "PJ Gate (Pintu Gerbang Jalan Universiti)", "Campus Gates & Vehicular Access Points", 3.1156, 101.6508, "PJ Gate (Pintu Gerbang Jalan Universiti)", 3.1156, 101.6508),
        CampusLocation("damansara_gate", "Damansara Gate (Pintu Gerbang Lingkungan Budi / Damansara)", "Campus Gates & Vehicular Access Points", 3.1359, 101.6577, "Damansara Gate (Pintu Gerbang Lingkungan Budi / Damansara)", 3.1359, 101.6577),
        CampusLocation("pantai_sentral_gate", "Pantai Sentral / Kerinchi Access Gate", "Campus Gates & Vehicular Access Points", 3.112, 101.6605, "Pantai Sentral / Kerinchi Access Gate", 3.112, 101.6605),
        CampusLocation("section_16_gate", "Section 16 / Jalan Ilmu Gate (Pintu Seksyen 16)", "Campus Gates & Vehicular Access Points", 3.1298, 101.644, "Section 16 / Jalan Ilmu Gate (Pintu Seksyen 16)", 3.1298, 101.644),
        CampusLocation("lrt_universiti", "KL Gateway - Universiti LRT Station (KJ19)", "Public Transit & Rail Stations (Feeder Hubs)", 3.1128, 101.6597, "KL Gateway - Universiti LRT Station (KJ19)", 3.1128, 101.6597),
        CampusLocation("lrt_kerinchi", "Kerinchi LRT Station (KJ18)", "Public Transit & Rail Stations (Feeder Hubs)", 3.1154, 101.668, "Kerinchi LRT Station (KJ18)", 3.1154, 101.668),
        CampusLocation("mrt_phileo", "Phileo Damansara MRT Station (KG12)", "Public Transit & Rail Stations (Feeder Hubs)", 3.1293, 101.6429, "Phileo Damansara MRT Station (KG12)", 3.1293, 101.6429),
        CampusLocation("lrt_abdullah_hukum", "Abdullah Hukum LRT & KTM Station (KJ17)", "Public Transit & Rail Stations (Feeder Hubs)", 3.119, 101.6735, "Abdullah Hukum LRT & KTM Station (KJ17)", 3.119, 101.6735),
        CampusLocation("tiara_damansara", "Tiara Damansara Condominium", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1246, 101.6383, "Tiara Damansara Condominium", 3.1246, 101.6383),
        CampusLocation("astana_damansara", "Astana Damansara Condominium", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1315, 101.6385, "Astana Damansara Condominium", 3.1315, 101.6385),
        CampusLocation("prima_16", "Prima 16 Condominium (Prima Sixteen)", "Off-Campus Student Residential Zones (Survey Aligned)", 3.13, 101.64, "Prima 16 Condominium (Prima Sixteen)", 3.13, 101.64),
        CampusLocation("li_villas", "Li Villas Condominium", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1255, 101.6418, "Li Villas Condominium", 3.1255, 101.6418),
        CampusLocation("pacific_star", "Pacific Star Business Hub & Residences", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1115, 101.6385, "Pacific Star Business Hub & Residences", 3.1115, 101.6385),
        CampusLocation("ryan_and_miho", "Ryan & Miho Residences", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1184, 101.6338, "Ryan & Miho Residences", 3.1184, 101.6338),
        CampusLocation("jaya_one_residences", "Jaya One Residences (The Suites)", "Off-Campus Student Residential Zones (Survey Aligned)", 3.118, 101.636, "Jaya One Residences (The Suites)", 3.118, 101.636),
        CampusLocation("atwater_pj", "Atwater Serviced Residences", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1178, 101.6323, "Atwater Serviced Residences", 3.1178, 101.6323),
        CampusLocation("pj_midtown", "PJ Midtown Residences", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1136, 101.6385, "PJ Midtown Residences", 3.1136, 101.6385),
        CampusLocation("centrestage_pj", "Centrestage Petaling Jaya", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1118, 101.639, "Centrestage Petaling Jaya", 3.1118, 101.639),
        CampusLocation("university_tower", "University Tower Condominium", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1135, 101.6501, "University Tower Condominium", 3.1135, 101.6501),
        CampusLocation("seksyen_11_pj", "Section 11 Petaling Jaya", "Off-Campus Student Residential Zones (Survey Aligned)", 3.115, 101.648, "Section 11 Petaling Jaya", 3.115, 101.648),
        CampusLocation("vista_angkasa", "Vista Angkasa Apartment", "Off-Campus Student Residential Zones (Survey Aligned)", 3.113, 101.6619, "Vista Angkasa Apartment", 3.113, 101.6619),
        CampusLocation("kl_gateway_residences", "KL Gateway Residences & Mall", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1132, 101.6635, "KL Gateway Residences & Mall", 3.1132, 101.6635),
        CampusLocation("residensi_kerinchi", "Residensi Kerinchi", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1135, 101.6645, "Residensi Kerinchi", 3.1135, 101.6645),
        CampusLocation("south_view_serviced_apartments", "South View Serviced Apartments", "Off-Campus Student Residential Zones (Survey Aligned)", 3.111, 101.6648, "South View Serviced Apartments", 3.111, 101.6648),
        CampusLocation("southlink_residences", "South Link Lifestyle Apartments", "Off-Campus Student Residential Zones (Survey Aligned)", 3.11, 101.6655, "South Link Lifestyle Apartments", 3.11, 101.6655),
        CampusLocation("novum_bangsar_south", "Novum Bangsar South", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1138, 101.6669, "Novum Bangsar South", 3.1138, 101.6669),
        CampusLocation("pantai_hillpark_phase1_3", "Pantai Hillpark (Phases 1, 2 & 3)", "Off-Campus Student Residential Zones (Survey Aligned)", 3.105, 101.6622, "Pantai Hillpark (Phases 1, 2 & 3)", 3.105, 101.6622),
        CampusLocation("pantai_hillpark_phase5_centrio", "Pantai Hillpark Phase 5 & Centrio", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1025, 101.665, "Pantai Hillpark Phase 5 & Centrio", 3.1025, 101.665),
        CampusLocation("bangsar_south_central", "Bangsar South Central (The Sphere & Nexus)", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1106, 101.6663, "Bangsar South Central (The Sphere & Nexus)", 3.1106, 101.6663),
        CampusLocation("pantai_panorama", "Pantai Panorama Condominium", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1085, 101.664, "Pantai Panorama Condominium", 3.1085, 101.664),
        CampusLocation("seksyen_17_central", "Seksyen 17 Commercial & Housing (Seventeen Mall)", "Off-Campus Student Residential Zones (Survey Aligned)", 3.1265, 101.6345, "Seksyen 17 Commercial & Housing (Seventeen Mall)", 3.1265, 101.6345),
        CampusLocation("other_kl_selangor", "Other KL / Selangor Area (Commuter Transit Gateway)", "Off-Campus Student Residential Zones (Survey Aligned)", null, null, "Other KL / Selangor Area (Commuter Transit Gateway)", null, null),
    )

    fun findById(id: String): CampusLocation? = allLocations.firstOrNull { it.id == id }

    fun search(query: String): List<CampusLocation> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allLocations
        return allLocations.filter {
            it.name.lowercase().contains(q) ||
            it.id.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
        }
    }
}
