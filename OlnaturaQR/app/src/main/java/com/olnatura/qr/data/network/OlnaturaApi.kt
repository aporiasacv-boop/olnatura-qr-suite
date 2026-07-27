package com.olnatura.qr.data.network

import com.olnatura.qr.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OlnaturaApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/v1/auth/me")
    suspend fun me(): MeResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("api/v1/auth/request-access")
    suspend fun requestAccess(@Body body: RequestAccessRequest): Response<RequestAccessResponse>

    @GET("api/v1/qr/{lote}")
    suspend fun getQr(@Path("lote") lote: String): QrResponse

    @POST("api/v1/scan/{lote}")
    suspend fun postScan(
        @Path("lote") lote: String,
        @Header("X-Device-Id") deviceId: String
    ): Response<Unit>

    @GET("api/v1/scan/{lote}")
    suspend fun getScanHistory(@Path("lote") lote: String): List<ScanEventResponse>

    @GET("api/v1/comments/{lote}")
    suspend fun getComments(@Path("lote") lote: String): List<LoteCommentResponse>

    @POST("api/v1/comments/{lote}")
    suspend fun postComment(
        @Path("lote") lote: String,
        @Body body: CreateLoteCommentRequest
    ): LoteCommentResponse

    @PATCH("api/v1/admin/lots/by-lote/{lote}/correct")
    suspend fun correctLabel(
        @Path("lote") lote: String,
        @Body body: AdminCorrectLabelRequest
    ): AdminCorrectLabelResponse

    @PATCH("api/v1/admin/lots/by-lote/{lote}/correct-status")
    suspend fun correctStatus(
        @Path("lote") lote: String,
        @Body body: AdminCorrectStatusRequest
    ): AdminCorrectStatusResponse
}