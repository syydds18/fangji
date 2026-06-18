package com.construction.diary.cloud.cloud

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class SignUpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)
data class SignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)
data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)
data class AuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: AuthUser? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_description") val errorDescription: String? = null
)
data class AuthUser(
    @SerializedName("id") val id: String = "",
    @SerializedName("email") val email: String = ""
)

interface SupabaseApi {
    @POST("auth/v1/signup")
    suspend fun signUp(@Header("apikey") apiKey: String, @Body request: SignUpRequest): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(@Header("apikey") apiKey: String, @Body request: SignInRequest): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(@Header("apikey") apiKey: String, @Body request: RefreshTokenRequest): Response<AuthResponse>

    @GET("rest/v1/{table}")
    suspend fun listRecords(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Path("table") table: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 10000
    ): Response<List<Map<String, Any?>>>

    @GET("rest/v1/{table}")
    suspend fun listRecordsByUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Path("table") table: String,
        @Query("select") select: String = "*",
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 10000
    ): Response<List<Map<String, Any?>>>

    @GET("rest/v1/{table}")
    suspend fun listRecordsByUserAndKey(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Path("table") table: String,
        @Query("select") select: String = "*",
        @Query("user_id") userIdFilter: String,
        @Query("key") keyFilter: String,
        @Query("limit") limit: Int = 10000
    ): Response<List<Map<String, Any?>>>

    /** 列表查询，不带默认排序（适用于无 created_at 列的表如 project_config） */
    @GET("rest/v1/{table}")
    suspend fun listRecordsByUserUnordered(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Path("table") table: String,
        @Query("select") select: String = "*",
        @Query("user_id") userIdFilter: String,
        @Query("limit") limit: Int = 10000
    ): Response<List<Map<String, Any?>>>

    @POST("rest/v1/{table}")
    suspend fun insertRecord(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Path("table") table: String,
        @Body record: Map<String, Any?>
    ): Response<List<Map<String, Any?>>>

    @POST("rest/v1/{table}")
    suspend fun upsertRecords(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Header("Content-Type") contentType: String = "application/json",
        @Path("table") table: String,
        @Body records: okhttp3.RequestBody
    ): Response<Unit>

    @DELETE("rest/v1/{table}")
    suspend fun deleteRecord(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Path("table") table: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Query("id") idFilter: String
    ): Response<Void>
}
