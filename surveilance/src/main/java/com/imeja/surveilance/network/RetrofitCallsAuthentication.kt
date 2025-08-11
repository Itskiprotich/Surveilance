package com.imeja.surveilance.network

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.imeja.surveilance.MainActivity
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.models.DbResetPasswordData
import com.imeja.surveilance.models.DbResponseError
import com.imeja.surveilance.models.DbSetPasswordReq
import com.imeja.surveilance.models.DbSignIn
import com.imeja.surveilance.models.DbUser
import com.imeja.surveilance.models.UrlData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Response

class RetrofitCallsAuthentication {

    fun loginUser(context: Context, dbSignIn: DbSignIn) {

        CoroutineScope(Dispatchers.Main).launch {
            val job = Job()
            CoroutineScope(Dispatchers.IO + job).launch { starLogin(context, dbSignIn) }.join()
        }
    }

    private fun starLogin(context: Context, dbSignIn: DbSignIn) {

        val job1 = Job()
        CoroutineScope(Dispatchers.Main + job1).launch {
            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Please wait..")
            progressDialog.setMessage("Authentication in progress..")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            var messageToast = ""
            val job = Job()
            CoroutineScope(Dispatchers.IO + job)
                .launch {
                    val formatter = FormatterClass()
                    val baseUrl = context.getString(UrlData.BASE_URL.message)
                    val apiService =
                        RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
                    try {

                        val apiInterface = apiService.signInUser(dbSignIn)
                        if (apiInterface.isSuccessful) {

                            val statusCode = apiInterface.code()
                            val body = apiInterface.body()

                            if (statusCode == 200 || statusCode == 201) {

                                if (body != null) {

                                    val access_token = body.access_token
                                    val expires_in = body.expires_in
                                    val refresh_expires_in = body.refresh_expires_in
                                    val refresh_token = body.refresh_token

                                    formatter.saveSharedPref("access_token", access_token, context)
                                    formatter.saveSharedPref(
                                        "expires_in",
                                        expires_in.toString(),
                                        context
                                    )
                                    formatter.saveSharedPref(
                                        "refresh_expires_in",
                                        refresh_expires_in,
                                        context
                                    )
                                    formatter.saveSharedPref(
                                        "refresh_token",
                                        refresh_token,
                                        context
                                    )
                                    formatter.saveSharedPref("isLoggedIn", "true", context)

                                    getUserDetails(context)

                                    messageToast = "Login successful.."

                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    )
                                    context.startActivity(intent)
                                    if (context is Activity) {
                                        context.finish()
                                    }
                                } else {
                                    messageToast = "Error: Body is null"
                                }
                            } else {
                                messageToast = "Error: The request was not successful"
                            }
                        } else {
                            apiInterface.errorBody()?.let {
                                //                  val errorBody = JSONObject(it.string())
                                messageToast =
                                    "Invalid login credentials. Please try again." // errorBody.getString("error")
                            }
                        }
                    } catch (e: Exception) {

                        Log.e("******", "")
                        Log.e("******", e.toString())
                        Log.e("******", "")

                        messageToast = "Cannot login user.."
                    }
                }
                .join()
            CoroutineScope(Dispatchers.Main).launch {
                progressDialog.dismiss()
                Toast.makeText(context, messageToast, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getUser(context: Context, dbSignIn: DbSignIn) {

        CoroutineScope(Dispatchers.Main).launch {
            val job = Job()
            CoroutineScope(Dispatchers.IO + job).launch { getUserDetails(context) }.join()
        }
    }

    private suspend fun getUserDetails(context: Context) {

        CoroutineScope(Dispatchers.IO).launch {
            val formatter = FormatterClass()
            val baseUrl = context.getString(UrlData.BASE_URL.message)
            val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
            try {

                val token = formatter.getSharedPref("access_token", context)
                if (token != null) {
                    val apiInterface = apiService.getUserInfo("Bearer $token")
                    if (apiInterface.isSuccessful) {

                        val statusCode = apiInterface.code()
                        val body = apiInterface.body()

                        if (statusCode == 200 || statusCode == 201) {

                            if (body != null) {

                                val user = body.user
                                if (user != null) {
                                    saveUserInformation(user, context)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {

                Log.e("******", "")
                Log.e("******", e.toString())
                Log.e("******", "")
            }
        }
    }

    private fun saveUserInformation(user: DbUser, context: Context) {
        val formatter = FormatterClass()

        val userDetails =
            mapOf(
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "role" to user.role,
                "id" to user.id,
                "idNumber" to user.idNumber,
                "fullNames" to user.fullNames,
                "phone" to (user.phone ?: ""), // Store empty string if null
                "email" to user.email
            )

        userDetails.forEach { (key, value) -> formatter.saveSharedPref(key, value, context) }

        // Optional debug log
        Log.d("UserPrefs", "User info saved to SharedPreferences: $userDetails")
    }

    fun getResetPassword(context: Context, dbResetPasswordData: DbResetPasswordData) = runBlocking {
        resetPassword(context, dbResetPasswordData)
    }

    private suspend fun resetPassword(
        context: Context,
        dbResetPasswordData: DbResetPasswordData
    ): Pair<Int, String> {

        var messageToast = ""
        var messageCode = 400

        val formatter = FormatterClass()
        val baseUrl = context.getString(UrlData.BASE_URL.message)
        val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
        try {
            val idNumber = dbResetPasswordData.idNumber
            val email = dbResetPasswordData.email

            val apiInterface = apiService.resetPassword(idNumber, email)
            if (apiInterface.isSuccessful) {

                val statusCode = apiInterface.code()
                val body = apiInterface.body()

                messageCode = statusCode

                messageToast =
                    if (statusCode == 200 || statusCode == 201) {
                        body?.response ?: "Cannot reset user password!"
                    } else {
                        "Cannot reset user password!!"
                    }
            } else {
                // Parse the error response
                val errorResponse = parseError(apiInterface)
                messageToast = errorResponse?.error ?: "Cannot reset user password! Try again"
            }
        } catch (e: Exception) {

            Log.e("******", "")
            Log.e("******", e.toString())
            Log.e("******", "")

            messageToast = "Cannot reset user password.."
        }
        return Pair(messageCode, messageToast)
    }

    // Parse error response using Gson
    private fun parseError(response: Response<*>): DbResponseError? {
        return try {
            response.errorBody()?.let {
                val gson = Gson()
                val type = object : TypeToken<DbResponseError>() {}.type
                gson.fromJson(it.charStream(), type)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setPassword(context: Context, dbSetPasswordReq: DbSetPasswordReq) = runBlocking {
        setPasswordBac(context, dbSetPasswordReq)
    }

    private suspend fun setPasswordBac(
        context: Context,
        dbSetPasswordReq: DbSetPasswordReq
    ): Pair<Int, String> {

        var messageToast = ""
        var messageCode = 400

        val formatter = FormatterClass()
        val baseUrl = context.getString(UrlData.BASE_URL.message)
        val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
        try {

            val apiInterface = apiService.setNewPassword(dbSetPasswordReq)
            if (apiInterface.isSuccessful) {

                val statusCode = apiInterface.code()
                val body = apiInterface.body()
                messageCode = statusCode

                messageToast =
                    if (statusCode == 200 || statusCode == 201) {
                        if (body != null) {
                            "Password Reset was successful.."
                        } else {
                            "Password Reset was not successful. Try again later"
                        }
                    } else {
                        "The request was not successful. Try again!"
                    }
            } else {
                // Parse the error response
                val errorResponse = parseError(apiInterface)
                messageToast = errorResponse?.error ?: "Cannot reset user password! Try again"
            }
        } catch (e: Exception) {

            Log.e("******", "")
            Log.e("******", e.toString())
            Log.e("******", "")

            messageToast = "Cannot set new password.."
        }
        return Pair(messageCode, messageToast)
    }
}