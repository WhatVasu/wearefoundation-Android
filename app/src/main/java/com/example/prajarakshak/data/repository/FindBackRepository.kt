package com.example.prajarakshak.data.repository

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import android.util.Log
import com.example.prajarakshak.data.api.ApiClient
import com.example.prajarakshak.data.model.CaseReport
import com.example.prajarakshak.data.model.AppNotification
import com.example.prajarakshak.data.model.PresignRequest
import com.example.prajarakshak.data.model.ReportRequest
import com.example.prajarakshak.data.model.ReportUpdateRequest
import com.example.prajarakshak.data.model.SearchRequest
import com.example.prajarakshak.data.model.SearchResponse
import com.example.prajarakshak.data.model.UserProfile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import kotlin.math.max

private val Context.preferences by preferencesDataStore(name = "findback_preferences")

class FindBackRepository(
    private val context: Context,
    private val auth: FirebaseAuth?
) {
    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        val configuredAuth = requireNotNull(auth) { firebaseSetupMessage }
        try {
            Tasks.await(configuredAuth.signInWithEmailAndPassword(email.trim(), password))
        } catch (exception: FirebaseAuthInvalidCredentialsException) {
            Log.e(
                "AuthError",
                "Error Code: ${exception.errorCode}, Message: ${exception.message}",
                exception
            )
            throw exception
        }
        Tasks.await(configuredAuth.currentUser!!.getIdToken(true))
        ApiClient.api().getProfile()
    }

    suspend fun signInWithGoogle(idToken: String) = withContext(Dispatchers.IO) {
        val configuredAuth = requireNotNull(auth) { firebaseSetupMessage }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Tasks.await(configuredAuth.signInWithCredential(credential))
        Tasks.await(requireNotNull(configuredAuth.currentUser).getIdToken(true))
        ApiClient.api().getProfile()
    }

    suspend fun authenticateWithPhone(
        credential: PhoneAuthCredential,
        linkToCurrentUser: Boolean
    ) = withContext(Dispatchers.IO) {
        val configuredAuth = requireNotNull(auth) { firebaseSetupMessage }
        val currentUser = configuredAuth.currentUser
        if (linkToCurrentUser) {
            val user = requireNotNull(currentUser) { "Sign in before linking a phone number." }
            Tasks.await(user.linkWithCredential(credential))
        } else {
            Tasks.await(configuredAuth.signInWithCredential(credential))
        }
        val user = requireNotNull(configuredAuth.currentUser)
        Tasks.await(user.reload())
        Tasks.await(user.getIdToken(true))
        val profile = ApiClient.api().getProfile()
        user.phoneNumber?.takeIf(String::isNotBlank)?.let { phoneNumber ->
            ApiClient.api().updateProfile(mapOf("phone" to phoneNumber))
        } ?: profile
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        district: String,
        password: String
    ) = withContext(Dispatchers.IO) {
        val configuredAuth = requireNotNull(auth) { firebaseSetupMessage }
        Tasks.await(configuredAuth.createUserWithEmailAndPassword(email.trim(), password))
        val user = requireNotNull(configuredAuth.currentUser)
        Tasks.await(user.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim())
            .build()))
        Tasks.await(user.getIdToken(true))
        ApiClient.api().updateProfile(
            mapOf("name" to name.trim(), "phone" to phone.trim(), "district" to district.trim())
        )
    }

    suspend fun sendPasswordReset(email: String) = withContext(Dispatchers.IO) {
        val configuredAuth = requireNotNull(auth) { firebaseSetupMessage }
        Tasks.await(configuredAuth.sendPasswordResetEmail(email.trim()))
    }

    suspend fun profile(): UserProfile = withContext(Dispatchers.IO) {
        ApiClient.api().getProfile()
    }

    suspend fun updateProfile(name: String, phone: String, district: String): UserProfile =
        withContext(Dispatchers.IO) {
            ApiClient.api().updateProfile(
                mapOf("name" to name.trim(), "phone" to phone.trim(), "district" to district.trim())
            )
        }

    suspend fun createReport(request: ReportRequest, photos: List<Uri>): CaseReport =
        withContext(Dispatchers.IO) {
            try {
                context.preferences.edit {
                    it[stringPreferencesKey("last_district")] = request.district
                }
                val api = ApiClient.api()
                val uploadedKeys = photos.map { uploadPhoto(api, it) }
                api.createReport(request.copy(imageKeys = uploadedKeys))
            } catch (exception: HttpException) {
                val responseBody = exception.response()?.errorBody()?.string()
                val apiMessage = responseBody?.let(::extractApiMessage)
                throw IllegalStateException(
                    apiMessage ?: "Report request failed (HTTP ${exception.code()}).",
                    exception
                )
            }
        }

    private fun extractApiMessage(responseBody: String): String? =
        try {
            JSONObject(responseBody).optString("message").takeIf(String::isNotBlank)
        } catch (_: JSONException) {
            null
        }

    suspend fun reports(): List<CaseReport> = withContext(Dispatchers.IO) {
        ApiClient.api().getMyReports()
    }

    suspend fun updateReport(reportId: String, description: String) = withContext(Dispatchers.IO) {
        ApiClient.api().updateReport(reportId, ReportUpdateRequest(description))
    }

    suspend fun search(request: SearchRequest): SearchResponse = withContext(Dispatchers.IO) {
        ApiClient.api().search(request)
    }

    suspend fun reportDetails(reportId: String): CaseReport = withContext(Dispatchers.IO) {
        ApiClient.api().getReportDetails(reportId)
    }

    suspend fun adminReports(status: String) = withContext(Dispatchers.IO) {
        ApiClient.api().getAdminReports(status).reports
    }

    suspend fun updateReportStatus(reportId: String, status: String) = withContext(Dispatchers.IO) {
        ApiClient.api().updateReportStatus(reportId, mapOf("status" to status))
    }

    suspend fun updateAdminReport(reportId: String, fields: Map<String, Any?>) =
        withContext(Dispatchers.IO) {
            ApiClient.api().updateAdminReport(reportId, fields)
        }

    suspend fun deleteAdminReport(reportId: String) = withContext(Dispatchers.IO) {
        ApiClient.api().deleteAdminReport(reportId)
    }

    suspend fun notifications(): List<AppNotification> = withContext(Dispatchers.IO) {
        ApiClient.api().getNotifications().notifications
    }

    suspend fun markNotificationRead(notificationId: String) = withContext(Dispatchers.IO) {
        ApiClient.api().markNotificationRead(notificationId)
    }

    private suspend fun uploadPhoto(
        api: com.example.prajarakshak.data.api.FindBackApi,
        uri: Uri
    ): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: error("The selected photo could not be opened.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected photo is invalid." }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > 3200) {
            sampleSize *= 2
        }
        val bitmap = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        } ?: error("The selected photo could not be decoded.")
        val scale = minOf(1f, 1600f / max(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap
        if (resized !== bitmap) bitmap.recycle()
        val compressed = try {
            ByteArrayOutputStream().use { output ->
                check(resized.compress(Bitmap.CompressFormat.JPEG, 82, output)) {
                    "The selected photo could not be compressed."
                }
                output.toByteArray()
            }
        } finally {
            resized.recycle()
        }
        require(compressed.size <= 8 * 1024 * 1024) { "Choose a smaller photo." }

        val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && column >= 0) cursor.getString(column) else null
        } ?: "report-photo.jpg"
        checkNotNull(auth?.currentUser) { "Sign in to upload a photograph." }
        val response = api.presignUpload(
            PresignRequest(fileName, contentLength = compressed.size)
        )
        val uploadRequest = Request.Builder()
            .url(response.uploadUrl)
            .put(compressed.toRequestBody("image/jpeg".toMediaType()))
            .build()
        okhttp3.OkHttpClient().newCall(uploadRequest).execute().use { result ->
            if (!result.isSuccessful) {
                val reason = when (result.code) {
                    400, 403 -> "The upload was rejected. Please retry or choose another photo."
                    404 -> "Photo storage is unavailable. Please contact support."
                    in 500..599 -> "Photo storage is temporarily unavailable. Please retry later."
                    else -> "The photo could not be uploaded. Please retry."
                }
                error("Photo upload failed: $reason")
            }
        }
        return response.objectKey
    }

    companion object {
        const val firebaseSetupMessage =
            "Firebase is not configured. Add firebaseApiKey, firebaseAppId and firebaseProjectId to local.properties."
    }
}
