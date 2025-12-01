package com.icl.demo.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.icl.demo.R
import com.icl.demo.utils.Constants.FIRST_LAUNCH_KEY
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale


class FormatterClass {
    private val dateInverseFormatSeconds: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)


    private val PREFSNAME = "facility_cache"
    private val KEYPREFIX = "facilities_"

    private val PREFNAME = "location_cache"
    private val KEYLOCATIONFACILITYMAP = "location_facility_map"

    fun saveFacilityIdsForWard(context: Context, locationId: String, facilityIds: List<String>) {
        val prefs = context.getSharedPreferences(PREFNAME, Context.MODE_PRIVATE)
        val existingJson = prefs.getString(KEYLOCATIONFACILITYMAP, "{}")
        val jsonObject = JSONObject(existingJson ?: "{}")

        jsonObject.put(locationId, JSONArray(facilityIds))
        prefs.edit().putString(KEYLOCATIONFACILITYMAP, jsonObject.toString()).apply()
    }

    fun getFacilityIdsForWard(context: Context, locationId: String): List<String>? {
        val prefs = context.getSharedPreferences(PREFNAME, Context.MODE_PRIVATE)
        val existingJson = prefs.getString(KEYLOCATIONFACILITYMAP, "{}")
        val jsonObject = JSONObject(existingJson ?: "{}")

        return if (jsonObject.has(locationId)) {
            val jsonArray = jsonObject.getJSONArray(locationId)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } else null
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFSNAME, Context.MODE_PRIVATE)

    fun saveFacilityIds(context: Context, wardId: String, facilityIds: List<String>) {
        val json = Gson().toJson(facilityIds)
        prefs(context).edit()
            .putString("$KEYPREFIX$wardId", json)
            .apply()
    }

    fun getFacilityIds(context: Context, wardId: String): List<String>? {
        val json = prefs(context).getString("$KEYPREFIX$wardId", null)
        return json?.let {
            Gson().fromJson(it, object : TypeToken<List<String>>() {}.type)
        }
    }

    fun String.toSlug(): String {
        return this
            .trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }


    fun saveSharedPref(key: String, value: String, context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getSharedPref(key: String, context: Context): String? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }

    fun deleteSharedPref(key: String, context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(key)
        editor.apply()
    }

    fun clearCache(context: Context) {
        val prefsLocal = context.getSharedPreferences(PREFNAME, Context.MODE_PRIVATE)
        prefs(context).edit().clear().apply()
        prefsLocal.edit().clear().apply()

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun isFirstLaunch(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        return sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(FIRST_LAUNCH_KEY, false)
        editor.apply()
    }

    /**
     * Formats the provided [date] into a string using the pattern
     * `yyyy-MM-dd HH:mm:ss` and the English locale.
     */
    fun formatDateTime(date: Date): String {
        return dateInverseFormatSeconds.format(date)
    }

    fun getTimeOfDay(): String {

        val currentTime = LocalTime.now()
        return when (currentTime.hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun resolveIcon(name: String): Int {
        return when (name.lowercase()) {
            "notifiable" -> R.drawable.virus
            "mass" -> R.drawable.syringe
            "case" -> R.drawable.clipboard
            "rcce" -> R.drawable.people
            "survey" -> R.drawable.presentation
            "add" -> R.drawable.useraddfill
            "view" -> R.drawable.received

            // default fallback — nothing dumb happens
            else -> 0
        }
    }
}