package com.example.prajarakshak.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prajarakshak.data.api.ApiClient
import com.example.prajarakshak.data.model.CaseReport
import com.example.prajarakshak.data.model.AppNotification
import com.example.prajarakshak.data.model.ReportRequest
import com.example.prajarakshak.data.model.SearchRequest
import com.example.prajarakshak.data.model.UserProfile
import com.example.prajarakshak.data.repository.FindBackRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException

enum class NoticeType { SUCCESS, ERROR, WARNING, INFORMATION }

data class UiNotice(val id: Long, val message: String, val type: NoticeType)

data class FindBackUiState(
    val loading: Boolean = false,
    val reportsLoading: Boolean = false,
    val reportsError: String? = null,
    val notificationsLoading: Boolean = false,
    val notificationsError: String? = null,
    val adminLoading: Boolean = false,
    val adminError: String? = null,
    val message: String? = null,
    val error: String? = null,
    val notice: UiNotice? = null,
    val reports: List<CaseReport> = emptyList(),
    val adminReports: List<CaseReport> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val searchResults: List<CaseReport> = emptyList(),
    val browseResults: List<CaseReport> = emptyList(),
    val searchFilters: SearchRequest = SearchRequest(),
    val browseDistrict: String = "",
    val browseLoading: Boolean = false,
    val browseError: String? = null,
    val browseHasMore: Boolean = false,
    val browsePage: Int = 0,
    val browseNextToken: String? = null,
    val searchLoading: Boolean = false,
    val searchError: String? = null,
    val searchHasMore: Boolean = false,
    val searchNextPage: Int = 1,
    val selectedReport: CaseReport? = null,
    val detailsLoading: Boolean = false,
    val detailsError: String? = null,
    val lastSubmittedReportId: String? = null,
    val lastSubmittedReportType: String? = null,
    val lastUpdatedReportId: String? = null,
    val profileUpdateVersion: Int = 0,
    val profile: UserProfile? = null,
    val user: FirebaseUser? = null,
)

data class ReportFormDraft(
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val hair: String = "",
    val clothing: String = "",
    val marks: String = "",
    val district: String = "",
    val location: String = "",
    val landmark: String = "",
    val date: String = "",
    val details: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val policeStation: String = "",
    val fir: String = "",
    val photoUris: List<String> = emptyList()
)

class FindBackViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = ApiClient.configureFirebase(application)
    private val repository = FindBackRepository(application, auth)
    private val _state = MutableStateFlow(FindBackUiState(user = auth?.currentUser))
    val state: StateFlow<FindBackUiState> = _state.asStateFlow()
    private val reportDrafts = mutableMapOf<String, ReportFormDraft>()
    private var reportSubmissionInProgress = false
    private var browseJob: Job? = null
    private var searchJob: Job? = null
    private var detailsJob: Job? = null
    private var reportsJob: Job? = null
    private var notificationsJob: Job? = null
    private var adminJob: Job? = null
    private var browseRequestId = 0
    private var searchRequestId = 0
    private var detailsRequestId = 0
    private var activeActions = 0
    private var noticeSequence = 0L

    init {
        if (auth?.currentUser != null) {
            runAction("Loading profile…", notifySuccess = false) {
                val profile = repository.profile()
                _state.value = _state.value.copy(profile = profile, user = auth.currentUser)
                val reports = repository.reports()
                _state.value = _state.value.copy(
                    reports = reports,
                    user = auth.currentUser
                )
                "Profile loaded."
            }
        }
    }

    fun signIn(email: String, password: String) = runAction("Signing in…") {
        val profile = repository.signIn(email, password)
        _state.value = _state.value.copy(profile = profile, user = auth?.currentUser)
        val reports = repository.reports()
        _state.value = _state.value.copy(
            reports = reports,
            user = auth?.currentUser
        )
        "Login successful! Welcome back."
    }

    fun signInWithGoogle(idToken: String) = runAction("Signing in with Google…") {
        val profile = repository.signInWithGoogle(idToken)
        _state.value = _state.value.copy(profile = profile, user = auth?.currentUser)
        val reports = repository.reports()
        _state.value = _state.value.copy(
            reports = reports,
            user = auth?.currentUser
        )
        "Login successful! Welcome back."
    }

    fun register(name: String, email: String, phone: String, district: String, password: String) =
        runAction("Creating account…") {
            val profile = repository.register(name, email, phone, district, password)
            _state.value = _state.value.copy(profile = profile, user = auth?.currentUser)
            "Account created successfully! Welcome! Your registration is complete."
        }

    fun updateProfile(name: String, phone: String, district: String) =
        runAction("Saving profile…") {
            val profile = repository.updateProfile(name, phone, district)
            _state.value = _state.value.copy(
                profile = profile,
                profileUpdateVersion = _state.value.profileUpdateVersion + 1
            )
            "Profile updated."
        }

    fun loadProfile() = runAction("Loading profile…") {
        val profile = repository.profile()
        _state.value = _state.value.copy(profile = profile, user = auth?.currentUser)
        "Profile loaded."
    }

    fun resetPassword(email: String) = runAction("Sending reset email…") {
        repository.sendPasswordReset(email)
        "Password reset email sent."
    }

    fun signOut() {
        auth?.signOut()
        reportDrafts.clear()
        _state.value = FindBackUiState(user = auth?.currentUser).copy(
            notice = nextNotice("Signed out successfully.", NoticeType.INFORMATION)
        )
    }

    fun loadReports() {
        reportsJob?.cancel()
        reportsJob = viewModelScope.launch {
            _state.value = _state.value.copy(reportsLoading = true, reportsError = null)
            try {
                val reports = repository.reports()
                _state.value = _state.value.copy(reports = reports, reportsLoading = false)
                notify(
                    if (reports.isEmpty()) "No reports found." else "Your reports are up to date.",
                    NoticeType.INFORMATION
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    reportsLoading = false,
                    reportsError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun updateReportDescription(reportId: String, description: String) {
        _state.value = _state.value.copy(lastUpdatedReportId = null)
        runAction("Saving report information…") {
            repository.updateReport(reportId, description)
            val reports = repository.reports()
            _state.value = _state.value.copy(reports = reports, lastUpdatedReportId = reportId)
            "Report information updated."
        }
    }

    fun updateSearchFilters(request: SearchRequest) {
        _state.value = _state.value.copy(searchFilters = request)
    }

    fun browseReports(
        district: String = _state.value.browseDistrict,
        refresh: Boolean = true
    ) {
        if (!refresh && _state.value.browseLoading) return
        if (refresh) browseJob?.cancel()
        val current = _state.value
        val page = if (refresh) 1 else current.browsePage + 1
        val nextToken = if (refresh) null else current.browseNextToken
        val requestId = ++browseRequestId
        browseJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                browseLoading = true,
                browseError = null,
                browseDistrict = district,
                browseResults = if (district != current.browseDistrict) emptyList() else current.browseResults
            )
            try {
                val response = repository.search(
                    SearchRequest(
                        mode = "browse",
                        district = district.trim().ifBlank { null },
                        pageSize = 20,
                        nextToken = nextToken
                    )
                )
                if (requestId != browseRequestId) return@launch
                _state.value = _state.value.copy(
                    browseLoading = false,
                    browseError = null,
                    browseResults = if (page == 1) response.results else _state.value.browseResults + response.results,
                    browseHasMore = response.hasMore,
                    browsePage = page,
                    browseNextToken = response.nextToken
                )
                if (response.results.isEmpty() && page == 1) {
                    notify("No verified reports found for this district.", NoticeType.INFORMATION)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (requestId != browseRequestId) return@launch
                _state.value = _state.value.copy(
                    browseLoading = false,
                    browseError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun search(request: SearchRequest) {
        searchJob?.cancel()
        val normalized = request.copy(mode = "match", page = 1)
        _state.value = _state.value.copy(searchFilters = normalized)
        val requestId = ++searchRequestId
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(searchLoading = true, searchError = null)
            try {
                val response = repository.search(normalized)
                if (requestId != searchRequestId) return@launch
                _state.value = _state.value.copy(
                    searchLoading = false,
                    searchError = null,
                    searchResults = response.results,
                    searchHasMore = response.hasMore,
                    searchNextPage = response.page + 1
                )
                if (response.results.isEmpty()) notify("No matching reports found.", NoticeType.INFORMATION)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (requestId != searchRequestId) return@launch
                _state.value = _state.value.copy(
                    searchLoading = false,
                    searchError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun searchMore() {
        val current = _state.value
        if (current.searchLoading || !current.searchHasMore) return
        val requestId = searchRequestId
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(searchLoading = true, searchError = null)
            try {
                val response = repository.search(
                    current.searchFilters.copy(page = current.searchNextPage)
                )
                if (requestId != searchRequestId) return@launch
                _state.value = _state.value.copy(
                    searchLoading = false,
                    searchError = null,
                    searchResults = current.searchResults + response.results,
                    searchNextPage = response.page + 1,
                    searchHasMore = response.hasMore
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (requestId != searchRequestId) return@launch
                _state.value = _state.value.copy(
                    searchLoading = false,
                    searchError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun loadReportDetails(reportId: String) {
        detailsJob?.cancel()
        val requestId = ++detailsRequestId
        detailsJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedReport = null,
                detailsLoading = true,
                detailsError = null
            )
            try {
                val response = repository.search(SearchRequest(mode = "details", reportId = reportId))
                if (requestId != detailsRequestId) return@launch
                _state.value = _state.value.copy(
                    selectedReport = response.results.firstOrNull(),
                    detailsLoading = false,
                    detailsError = if (response.results.isEmpty()) "This report is unavailable." else null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (requestId != detailsRequestId) return@launch
                _state.value = _state.value.copy(
                    detailsLoading = false,
                    detailsError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun clearSelectedReport() {
        detailsJob?.cancel()
        detailsRequestId++
        _state.value = _state.value.copy(
            selectedReport = null,
            detailsLoading = false,
            detailsError = null
        )
    }

    private fun reportDraftKey(missing: Boolean, potentialCaseId: String?): String =
        potentialCaseId?.let { "SIGHTING:$it" } ?: if (missing) "MISSING" else "FOUND"

    fun reportDraft(missing: Boolean, potentialCaseId: String? = null): ReportFormDraft =
        reportDrafts[reportDraftKey(missing, potentialCaseId)] ?: ReportFormDraft()

    fun saveReportDraft(
        missing: Boolean,
        draft: ReportFormDraft,
        potentialCaseId: String? = null
    ) {
        reportDrafts[reportDraftKey(missing, potentialCaseId)] = draft
    }

    fun resetSubmissionState() {
        _state.value = _state.value.copy(
            lastSubmittedReportId = null,
            lastSubmittedReportType = null
        )
    }

    fun loadAdminReports(status: String = "SUBMITTED") {
        adminJob?.cancel()
        _state.value = _state.value.copy(
            adminReports = emptyList(),
            adminLoading = true,
            adminError = null
        )
        adminJob = viewModelScope.launch {
            try {
                val reports = repository.adminReports(status)
                _state.value = _state.value.copy(adminReports = reports, adminLoading = false)
                notify(
                    if (reports.isEmpty()) "No reports in this review queue." else "Review queue loaded.",
                    NoticeType.INFORMATION
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    adminLoading = false,
                    adminError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun loadNotifications() {
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            _state.value = _state.value.copy(notificationsLoading = true, notificationsError = null)
            try {
                val notifications = repository.notifications()
                _state.value = _state.value.copy(
                    notifications = notifications,
                    notificationsLoading = false
                )
                notify(
                    if (notifications.isEmpty()) "No notifications found." else "Notifications loaded.",
                    NoticeType.INFORMATION
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    notificationsLoading = false,
                    notificationsError = errorMessage(exception)
                )
                notify(errorMessage(exception), NoticeType.ERROR)
            }
        }
    }

    fun markNotificationRead(notificationId: String) = runAction("Updating notification…") {
        repository.markNotificationRead(notificationId)
        _state.value = _state.value.copy(
            notifications = _state.value.notifications.map {
                if (it.notificationId == notificationId) it.copy(read = true) else it
            }
        )
        "Notification marked read."
    }

    fun updateReportStatus(reportId: String, status: String, queueStatus: String = "SUBMITTED") =
        runAction("Updating case status…") {
            repository.updateReportStatus(reportId, status)
            val reports = repository.adminReports(queueStatus)
            _state.value = _state.value.copy(adminReports = reports)
            "Case status updated."
        }

    fun updateAdminReport(reportId: String, fields: Map<String, Any?>, queueStatus: String) =
        runAction("Saving report changes…") {
            _state.value = _state.value.copy(lastUpdatedReportId = null)
            repository.updateAdminReport(reportId, fields)
            val reports = repository.adminReports(queueStatus)
            _state.value = _state.value.copy(
                adminReports = reports,
                lastUpdatedReportId = reportId
            )
            "Report updated successfully."
        }

    fun deleteAdminReport(reportId: String, queueStatus: String) =
        runAction("Deleting report…") {
            val result = repository.deleteAdminReport(reportId)
            _state.value = _state.value.copy(
                adminReports = _state.value.adminReports.filterNot { it.reportId == reportId }
            )
            val reports = repository.adminReports(queueStatus)
            _state.value = _state.value.copy(adminReports = reports)
            if (result["photoCleanupPending"] == "true") {
                "Report deleted, but some photos could not be removed. Contact support."
            } else {
                "Report deleted successfully."
            }
        }

    fun submitReport(request: ReportRequest, photos: List<android.net.Uri>) {
        if (reportSubmissionInProgress) return
        reportSubmissionInProgress = true
        runAction("Submitting report…") {
            try {
            val result = repository.createReport(request, photos)
            reportDrafts.remove(
                reportDraftKey(request.reportType == "MISSING", request.potentialMatchReportId)
            )
            _state.value = _state.value.copy(
                reports = listOf(result) + _state.value.reports,
                message = "Report submitted. Reference: ${result.reportId}",
                lastSubmittedReportId = result.reportId,
                lastSubmittedReportType = request.reportType
            )
            "Report submitted. Reference: ${result.reportId}"
            } finally {
                reportSubmissionInProgress = false
            }
        }
    }

    fun notify(message: String, type: NoticeType) {
        if (_state.value.notice?.let { it.message == message && it.type == type } == true) return
        _state.value = _state.value.copy(notice = nextNotice(message, type))
    }

    fun dismissNotice(id: Long) {
        if (_state.value.notice?.id == id) {
            _state.value = _state.value.copy(notice = null)
        }
    }

    private fun nextNotice(message: String, type: NoticeType): UiNotice {
        _state.value.notice?.let {
            if (it.message == message && it.type == type) return it
        }
        return UiNotice(id = ++noticeSequence, message = message, type = type)
    }

    private fun runAction(
        progress: String,
        notifySuccess: Boolean = true,
        successType: NoticeType = NoticeType.SUCCESS,
        action: suspend () -> String
    ) {
        viewModelScope.launch {
            activeActions++
            _state.value = _state.value.copy(loading = true, message = progress, error = null)
            try {
                val message = action()
                _state.value = _state.value.copy(
                    message = message,
                    notice = if (notifySuccess && message.isNotBlank()) {
                        nextNotice(message, successType)
                    } else {
                        _state.value.notice
                    }
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val httpStatus = generateSequence<Throwable>(exception) { it.cause }
                    .filterIsInstance<HttpException>()
                    .firstOrNull()
                    ?.code()
                if (httpStatus == 401) {
                    auth?.signOut()
                    reportDrafts.clear()
                }
                val currentUser = auth?.currentUser
                val userMessage = errorMessage(exception)
                _state.value = _state.value.copy(
                    message = null,
                    error = userMessage,
                    notice = nextNotice(userMessage, noticeType(exception)),
                    user = currentUser,
                    profile = if (currentUser?.uid == _state.value.user?.uid) {
                        _state.value.profile
                    } else {
                        null
                    }
                )
            } finally {
                activeActions = (activeActions - 1).coerceAtLeast(0)
                _state.value = _state.value.copy(loading = activeActions > 0)
            }
        }
    }

    private fun noticeType(exception: Exception): NoticeType =
        if (exception is IOException) {
            NoticeType.WARNING
        } else {
            NoticeType.ERROR
        }

    private fun errorMessage(exception: Exception): String {
        val authError = generateSequence<Throwable>(exception) { it.cause }
            .filterIsInstance<FirebaseAuthException>()
            .firstOrNull()
        if (authError != null) {
            return when (authError.errorCode) {
                "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" ->
                    "Email or password is incorrect."
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                "ERROR_USER_DISABLED" -> "This account is disabled. Contact support."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many unsuccessful attempts. Wait and try again."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
                "ERROR_WEAK_PASSWORD" -> "Choose a stronger password with at least 8 characters."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network connection failed. Check your connection and retry."
                else -> "Authentication could not be completed. Please try again."
            }
        }

        val httpError = generateSequence<Throwable>(exception) { it.cause }
            .filterIsInstance<HttpException>()
            .firstOrNull()
        if (httpError != null) {
            val backendMessage = httpError.response()?.errorBody()?.string()?.let(::parseApiMessage)
            return when (httpError.code()) {
                400 -> backendMessage ?: "Some information is invalid. Check the form and try again."
                401 -> "Your session expired. Sign in again."
                403 -> "Access denied. You do not have permission to do this."
                404 -> backendMessage ?: "The requested record was not found."
                409 -> backendMessage ?: "This record conflicts with a recent change. Refresh and retry."
                422 -> backendMessage ?: "The information could not be processed. Check the form."
                429 -> "Too many requests. Wait a moment and retry."
                in 500..599 -> "The service is temporarily unavailable. Please try again later."
                else -> backendMessage ?: "The request failed (HTTP ${httpError.code()})."
            }
        }

        if (generateSequence<Throwable>(exception) { it.cause }.any { it is IOException }) {
            return "Network connection failed. Check your connection and retry."
        }

        val localMessage = exception.message.orEmpty()
        val safeLocalMessage = localMessage.takeIf {
            it == FindBackRepository.firebaseSetupMessage ||
                it == "The selected photo could not be opened." ||
                it == "The selected photo is invalid." ||
                it == "The selected photo could not be decoded." ||
                it == "The selected photo could not be compressed." ||
                it == "Choose a smaller photo." ||
                it == "Sign in to upload a photograph." ||
                it.startsWith("Photo upload failed:") ||
                it.startsWith("Report request failed (HTTP ")
        }
        return safeLocalMessage ?: "The request could not be completed. Please try again."
    }

    private fun parseApiMessage(body: String): String? =
        try {
            JSONObject(body).optString("message").takeIf(String::isNotBlank)
        } catch (_: JSONException) {
            null
        }
}
