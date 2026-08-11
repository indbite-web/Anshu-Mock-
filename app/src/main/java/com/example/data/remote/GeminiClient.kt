package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val fallbackOrder = SupportedModel.FREE_MODEL_ALLOWLIST

    @Volatile
    private var cachedAvailableModels: List<SupportedModel>? = null

    suspend fun fetchAvailableFreeModels(apiKey: String, forceRefresh: Boolean = false): List<SupportedModel> {
        val cached = cachedAvailableModels
        if (!forceRefresh && cached != null) {
            return cached
        }
        if (apiKey.isBlank()) return SupportedModel.FREE_MODEL_ALLOWLIST

        return try {
            val response = apiService.listModels(apiKey)
            val body = response.body()
            if (response.isSuccessful && body?.models != null) {
                val remoteModels = body.models
                val available = SupportedModel.FREE_MODEL_ALLOWLIST.filter { allowlisted ->
                    remoteModels.any { remote ->
                        val cleanName = remote.name.removePrefix("models/")
                        cleanName.equals(allowlisted.modelId, ignoreCase = true) &&
                                (remote.supportedGenerationMethods?.contains("generateContent") != false)
                    }
                }
                val result = if (available.isNotEmpty()) available else SupportedModel.FREE_MODEL_ALLOWLIST
                cachedAvailableModels = result
                result
            } else {
                SupportedModel.FREE_MODEL_ALLOWLIST
            }
        } catch (e: Exception) {
            SupportedModel.FREE_MODEL_ALLOWLIST
        }
    }

    suspend fun testConnection(apiKey: String, model: SupportedModel): Pair<Boolean, String> {
        if (apiKey.isBlank()) {
            return Pair(false, "Not Configured")
        }
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Hello! Briefly confirm connection in 1 word."))
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.1f)
        )

        return try {
            val response = apiService.generateContent(model.modelId, apiKey, request)
            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                Pair(true, "Connected")
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                when {
                    code in listOf(400, 401, 403) || errBody.contains("API_KEY_INVALID", ignoreCase = true) || errBody.contains("API key not valid", ignoreCase = true) ->
                        Pair(false, "Invalid API Key")
                    code == 429 || errBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || errBody.contains("quota", ignoreCase = true) ->
                        Pair(false, "Free Tier quota/rate limit reached")
                    code == 404 || errBody.contains("MODEL_NOT_FOUND", ignoreCase = true) ->
                        Pair(false, "Selected model unavailable")
                    else ->
                        Pair(false, "Invalid API Key")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Network unavailable")
        }
    }
}
