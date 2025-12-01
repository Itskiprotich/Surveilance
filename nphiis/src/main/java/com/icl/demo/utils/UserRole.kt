package com.icl.demo.utils

enum class UserRole(val key: String) {
    ADMINISTRATOR("administrator"),
    SUPERUSER("superuser"),
    COUNTY_DISEASE_SURVEILLANCE_OFFICER("county_user"),
    SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER("sub_county_user"),
    FACILITY_SURVEILLANCE_FOCAL_PERSON("facility_nurse"),
    SUPERVISOR("supervisor"),
    VACCINATOR("vaccinator");

    companion object {
        fun fromAny(value: String): UserRole? =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                        it.key.equals(value, ignoreCase = true)
            }

        fun fromKey(key: String): UserRole? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}

enum class LocationLevel(val level: Int) {
    NATIONAL(0),
    COUNTY(1),
    SUB_COUNTY(2),
    WARD(3),
    FACILITY(4)
}