package com.imeja.surveilance.helpers

object Constants {

    //MOH 505
    const val COUNTY = "a4-county"
    const val SUB_COUNTY = "a3-sub-county"
    const val WARD = "819943434"
    const val HEALTH_FACILITY = "819946803677"
    const val FACILITY_TYPE = "438862163919"
    const val WEEK_ENDING_DATE = "728034137219"

    val FACILITY_DETAILS =
        listOf(COUNTY, SUB_COUNTY, WARD, HEALTH_FACILITY, FACILITY_TYPE, WEEK_ENDING_DATE)

    const val AEFI = "aefi-summary"
    const val BACTERIAL_MENINGITIS = "bacterial-meningitis-summary"
    const val ACUTE_JAUNDICE = "acute-jaundice-summary"
    const val NEONATAL_DEATHS = "neonatal-deaths-summary"
    const val ACUTE_MALNUTRITION = "acute-malnutrition-summary"
    const val CHIKUNGUNYA = "chikungunya-summary"
    const val COVID_19 = "covid--19-summary"
    const val SARI_CLUSTER = "sari-cluster-ge3-cases-summary"
    const val DENGUE = "dengue-summary"
    const val MEASLES = "measles-summary"
    const val RIFT_VALLEY_FEVER = "rift-valley-fever-summary"
    const val TYPHOID = "typhoid-summary"
    const val ANTHRAX = "anthrax-summary"
    const val GUINEA_WORM = "guinea-worm-disease-summary"
    const val VHF = "vhf-summary"
    const val ZIKA = "zika-virus-summary"
    const val SUSPECTED_MALARIA = "suspected-malaria-summary"
    const val YELLOW_FEVER = "yellow-fever-summary"
    const val SUSPECTED_MDR_XDR_TB = "suspected-mdr-xdr-tb-summary"
    const val OTHERS = "others-specify-summary"

    val ALL = listOf(
        AEFI,
        BACTERIAL_MENINGITIS,
        ACUTE_JAUNDICE,
        NEONATAL_DEATHS,
        ACUTE_MALNUTRITION,
        CHIKUNGUNYA,
        COVID_19,
        SARI_CLUSTER,
        DENGUE,
        MEASLES,
        RIFT_VALLEY_FEVER,
        TYPHOID,
        ANTHRAX,
        GUINEA_WORM,
        VHF,
        ZIKA,
        SUSPECTED_MALARIA,
        YELLOW_FEVER,
        SUSPECTED_MDR_XDR_TB,
        OTHERS
    )

    val ALL_LINK_IDS = FACILITY_DETAILS + ALL

    val MPOX_GUIDES = listOf(
        "294367770999",  // County
        "819946803642",  // Subcounty
        "819943434",     // Ward
        "village",       // Village
        "team_no",       // Team No
        "team_type",     // Team type
        "campaign_day",  // Campaign Day
        "728034137219",  // Date
        "hcw_18_39_reported",       // Were any healthcare workers aged 18–39 years reported?
        "hcw_40_59_reported",       // Were any healthcare workers aged 40–59 years reported?
        "hcw_60_plus_reported",     // Were any healthcare workers aged 60+ years reported?
        "sw_18_39_reported",        // Were any sex workers aged 18–39 years reported?
        "sw_40_59_reported",        // Were any sex workers aged 40–59 years reported?
        "sw_60_plus_reported",      // Were any sex workers aged 60+ years reported?
        "td_18_39_reported",        // Were any truck drivers aged 18–39 years reported?
        "td_40_59_reported",        // Were any truck drivers aged 40–59 years reported?
        "td_60_plus_reported",      // Were any truck drivers aged 60+ years reported?
        "others_18_39_reported",    // Were any others aged 18–39 years reported?
        "others_40_59_reported",    // Were any others aged 40–59 years reported?
        "others_60_plus_reported",  // Were any others aged 60+ years reported?
        "aefi_yes_no"               // Reported AEFI?

    )

    val ALL_MPOX_LINK_IDS = FACILITY_DETAILS + MPOX_GUIDES
}

