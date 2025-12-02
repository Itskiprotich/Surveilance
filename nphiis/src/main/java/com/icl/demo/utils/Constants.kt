package com.icl.demo.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.icl.demo.R
import androidx.core.content.edit


object Constants {

    const val BASE_URL = "https://dsrfhir.intellisoftkenya.com/hapi/fhir/"
//    const val BASE_URL = "http://45.79.161.190:8085/fhir/"

    //        const val BASE_URL ="https://auth.nphiis.nphl.go.ke/fhir/" LIVE

    const val BASE_API_URL = "https://dsrkeycloak.intellisoftkenya.com/auth/"

    //    const val BASE_API_URL="https://auth.nphiis.nphl.go.ke/" LIVE
    const val ALERTS_BASE_URL = "https://dsrfhir.intellisoftkenya.com/api/"

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


    const val LOCATION_STARTER = "${BASE_URL}Location?_count=200&_offset=0"
    const val NOTIFICATION_CODE = 1001
    const val FIRST_LAUNCH_KEY = "is_first_launch"
    private const val PREF_NAME = "pagination_pref"
    private const val KEY_NEXT_URL = "next_url"
    private const val KEY_DOWNLOAD_COMPLETE = "download_complete"

    const val TEST_TOKEN =
        "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxbTVfeFpQWFVHV1I0YTdwbkpLZ1VORVRNbERMMlpHeXhUSndNSEx5UkhjIn0.eyJleHAiOjE3NzYwNTkxMTMsImlhdCI6MTc2MDUwNzExMywianRpIjoiYmQ5YTdiOGItOWFlNS00ZmJkLWFiY2YtODY4MTFmNzhlYjJhIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL21hc3RlciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIwZGMzM2ExYi1iNmJjLTRjZTgtYjU5MS1iYTU4ZDZmYTFhZTYiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjaGFuam8tY2xpZW50LWFwaXMiLCJzaWQiOiIxZmNlOTRlNi1lYjRjLTRkMjItYThmNS00ZjhmZTIxYTIxMDMiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgb3JnYW5pemF0aW9uIHByb2ZpbGUgb3BlbmlkIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiS2lwcm90aWNoIEphcGhldGgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiI0MTQxNDEiLCJnaXZlbl9uYW1lIjoiS2lwcm90aWNoIiwiZmFtaWx5X25hbWUiOiJKYXBoZXRoIiwiZW1haWwiOiJqa2lwcm90aWMuaEBpbnRlbGxpc29mdGtlbnlhLmNvbSJ9.ZrQm4Pzd-AtjbbMkWj6DR_cK2CZTtQ2f1mE58AavL9FoReCAUKX2ze1TsqRe93NbtWfOUSwUee7ks-SlD9JaxmsEuVcY6YI5uA4rFw_CJ4zu9NB6oCCOzf1ZEQRkgeqHFKw1KF2ciDvnJaQwsDJjW8Hd6MAHzX8GI4iFBp7h_LT1zL_oCb6jMzPOfatOi5RAGisdy3bUFHzRL5IUZlOwqjMA4QxGHeSeAV-Hbegrjtfx-ufUdiITLVU4DYXYwReKth_D9iitFh8ul5kx8CZH8sUPbD5d2iqbw6HMH6_-Rb_sjj3mnJTjrct98UCOHIUrAmM4UOmNxgD9xYc0ZA4puw"

    fun saveNextUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_NEXT_URL, url) }
    }

    fun getNextUrl(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NEXT_URL, null)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        prefs.edit { remove(KEY_NEXT_URL) }
    }

    fun markActionComplete(context: Context, action: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        sharedPreferences.edit {
            putBoolean(KEY_DOWNLOAD_COMPLETE, action)
        }

    }

    fun isDownloadComplete(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_DOWNLOAD_COMPLETE, false)
    }
}