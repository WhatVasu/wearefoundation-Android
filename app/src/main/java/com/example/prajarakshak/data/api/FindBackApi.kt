package com.example.prajarakshak.data.api

import com.example.prajarakshak.data.model.CaseReport
import com.example.prajarakshak.data.model.AdminReportsResponse
import com.example.prajarakshak.data.model.NotificationsResponse
import com.example.prajarakshak.data.model.PresignRequest
import com.example.prajarakshak.data.model.PresignResponse
import com.example.prajarakshak.data.model.ReportRequest
import com.example.prajarakshak.data.model.ReportUpdateRequest
import com.example.prajarakshak.data.model.SearchRequest
import com.example.prajarakshak.data.model.SearchResponse
import com.example.prajarakshak.data.model.UserProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FindBackApi {
    @GET("me")
    suspend fun getProfile(): UserProfile

    @PATCH("me")
    suspend fun updateProfile(@Body fields: Map<String, String>): UserProfile

    @POST("reports")
    suspend fun createReport(@Body request: ReportRequest): CaseReport

    @PATCH("reports/{id}")
    suspend fun updateReport(
        @Path("id") reportId: String,
        @Body request: ReportUpdateRequest
    ): CaseReport

    @GET("reports/my")
    suspend fun getMyReports(): List<CaseReport>

    @GET("reports/{id}")
    suspend fun getReportDetails(@Path("id") reportId: String): CaseReport

    @POST("search")
    suspend fun search(@Body request: SearchRequest): SearchResponse

    @POST("uploads/presign")
    suspend fun presignUpload(@Body request: PresignRequest): PresignResponse

    @GET("admin/reports")
    suspend fun getAdminReports(
        @retrofit2.http.Query("status") status: String = "SUBMITTED",
        @retrofit2.http.Query("district") district: String? = null
    ): AdminReportsResponse

    @PATCH("admin/reports/{id}/status")
    suspend fun updateReportStatus(
        @Path("id") reportId: String,
        @Body status: Map<String, String>
    ): CaseReport

    @PATCH("admin/reports/{id}")
    suspend fun updateAdminReport(
        @Path("id") reportId: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): CaseReport

    @DELETE("admin/reports/{id}")
    suspend fun deleteAdminReport(@Path("id") reportId: String): Map<String, String>

    @GET("notifications")
    suspend fun getNotifications(): NotificationsResponse

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") notificationId: String): Map<String, Any>
}
