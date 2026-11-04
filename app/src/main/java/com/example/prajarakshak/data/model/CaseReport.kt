package com.example.prajarakshak.data.model

data class CaseReport(
    val reportId: String,
    val reportType: String,
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val district: String? = null,
    val location: String? = null,
    val description: String? = null,
    val imageUrls: List<String>? = emptyList(),
    val status: String = "Submitted",
    val createdAt: String? = null,
    val matchingSignals: List<String>? = emptyList(),
    val statusHistory: List<StatusEvent>? = emptyList(),
    val physicalFeatures: Map<String, String>? = emptyMap(),
    val lastSeenDate: String? = null,
    val potentialMatchReportId: String? = null
)

data class StatusEvent(val status: String, val at: String? = null)

data class ReportUpdateRequest(val description: String)

data class ReportRequest(
    val reportType: String,
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val physicalFeatures: Map<String, String> = emptyMap(),
    val district: String,
    val location: String,
    val description: String,
    val imageKeys: List<String> = emptyList(),
    val lastSeenDate: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val policeStation: String? = null,
    val firReference: String? = null,
    val potentialMatchReportId: String? = null
)

data class SearchRequest(
    val mode: String? = null,
    val reportId: String? = null,
    val referenceId: String? = null,
    val name: String? = null,
    val district: String? = null,
    val location: String? = null,
    val area: String? = null,
    val lastSeenLocation: String? = null,
    val hair: String? = null,
    val height: String? = null,
    val mole: String? = null,
    val scar: String? = null,
    val birthmark: String? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val gender: String? = null,
    val features: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val nextToken: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)

data class SearchResponse(
    val results: List<CaseReport> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val nextToken: String? = null
)

data class UserProfile(
    val userId: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val district: String? = null,
    val role: String = "citizen"
)

data class PresignRequest(
    val fileName: String,
    val contentType: String = "image/jpeg",
    val contentLength: Int
)

data class PresignResponse(
    val uploadUrl: String,
    val objectKey: String
)

data class AdminReportsResponse(
    val reports: List<CaseReport> = emptyList()
)

data class AppNotification(
    val notificationId: String,
    val message: String,
    val read: Boolean = false,
    val createdAt: String? = null
)

data class NotificationsResponse(
    val notifications: List<AppNotification> = emptyList()
)
