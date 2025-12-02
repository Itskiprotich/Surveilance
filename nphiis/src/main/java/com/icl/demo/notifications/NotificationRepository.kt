package com.icl.demo.notifications

import android.content.Context
import com.icl.demo.models.NotificationResponse
import com.icl.demo.network.Interface
import com.icl.demo.network.RetrofitBuilder
import com.icl.demo.utils.Constants.ALERTS_BASE_URL
import com.icl.demo.utils.FormatterClass


class NotificationRepository {
    suspend fun getNotifications(context: Context): NotificationResponse? {

        val formatter = FormatterClass()
        val apiService =
            RetrofitBuilder.getRetrofit(ALERTS_BASE_URL).create(Interface::class.java)
        try {

            val token = formatter.getSharedPref("access_token", context)
            if (token != null) {
                val apiInterface = apiService.pullUserAlerts("Bearer $token")
                if (apiInterface.isSuccessful) {

                    val statusCode = apiInterface.code()
                    val body = apiInterface.body()

                    return if (statusCode == 200 || statusCode == 201) {

                        body
                    } else {
                        null
                    }
                } else {
                    return null
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }
}