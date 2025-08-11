package com.imeja.surveilance.network

import com.imeja.surveilance.models.DbResetPassword
import com.imeja.surveilance.models.DbSetPasswordReq
import com.imeja.surveilance.models.DbSignIn
import com.imeja.surveilance.models.DbSignInResponse
import com.imeja.surveilance.models.DbUserInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface Interface {

    @POST("provider/login")
    suspend fun signInUser(@Body dbSignIn: DbSignIn): Response<DbSignInResponse>

    @GET("provider/me")
    suspend fun getUserInfo(
        @Header("Authorization") token: String,
    ): Response<DbUserInfoResponse>

    @GET("provider/reset-password")
    suspend fun resetPassword(
        @Query("idNumber") idNumber: String,
        @Query("email", encoded = true) email: String,
    ): Response<DbResetPassword>

    @POST("provider/reset-password")
    suspend fun setNewPassword(@Body dbSetPasswordReq: DbSetPasswordReq): Response<Any>
}