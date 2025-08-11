package com.imeja.surveilance.helpers


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.imeja.surveilance.R
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

class FormatterClass {
    private val dateInverseFormatSeconds: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

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

    fun formatCurrentDateTime(date: Date): String {
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
}