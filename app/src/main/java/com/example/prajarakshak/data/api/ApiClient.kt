package com.example.prajarakshak.data.api

import android.content.Context
import com.example.prajarakshak.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var api: FindBackApi? = null
    @Volatile
    private var firebaseAuth: FirebaseAuth? = null

    @Synchronized
    fun api(): FindBackApi {
        val baseUrl = BuildConfig.API_BASE_URL
        require(baseUrl.startsWith("https://") && baseUrl.endsWith("/")) {
            "Set findbackApiBaseUrl to your deployed HTTPS API URL (including trailing /) in local.properties."
        }
        return api ?: Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(Interceptor { chain ->
                        val request = chain.request().newBuilder()
                        firebaseAuth?.currentUser?.let { user ->
                            val token = Tasks.await(user.getIdToken(false)).token
                                ?: error("Firebase did not return an ID token.")
                            request.header("Authorization", "Bearer $token")
                        }
                        chain.proceed(request.build())
                    })
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FindBackApi::class.java)
            .also { api = it }
    }

    fun configureFirebase(context: Context): FirebaseAuth? {
        val existing = FirebaseApp.getApps(context).firstOrNull()
        if (existing != null) {
            return FirebaseAuth.getInstance(existing).also { firebaseAuth = it }
        }
        if (BuildConfig.FIREBASE_API_KEY.isBlank() ||
            BuildConfig.FIREBASE_APP_ID.isBlank() ||
            BuildConfig.FIREBASE_PROJECT_ID.isBlank()
        ) return null

        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .build()
        return FirebaseApp.initializeApp(context, options)
            ?.let(FirebaseAuth::getInstance)
            ?.also { firebaseAuth = it }
    }
}
