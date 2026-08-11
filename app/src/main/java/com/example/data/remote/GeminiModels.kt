package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String? = "user",
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "mimeType") val mimeType: String = "application/json"
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @Json(name = "temperature") val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = GenerationConfig(),
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null,
    @Json(name = "error") val error: GeminiApiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class ModelInfo(
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ListModelsResponse(
    @Json(name = "models") val models: List<ModelInfo>? = null
)

enum class SupportedModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Recommended for fast & accurate multimodal MCQs"
    ),
    GEMINI_3_6_FLASH(
        modelId = "gemini-3.6-flash",
        displayName = "Gemini 3.6 Flash",
        description = "High intelligence, fast multimodal performance"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "Lightweight, high-speed text & vision processing"
    ),
    GEMINI_FLASH_LATEST(
        modelId = "gemini-flash-latest",
        displayName = "Gemini Flash Latest",
        description = "Latest stable Gemini Flash build"
    );

    companion object {
        val FREE_MODEL_ALLOWLIST = listOf(
            GEMINI_3_5_FLASH,
            GEMINI_3_6_FLASH,
            GEMINI_3_5_FLASH_LITE,
            GEMINI_FLASH_LATEST
        )

        val DEFAULT_MODEL = GEMINI_3_5_FLASH

        fun fromModelId(id: String): SupportedModel {
            return FREE_MODEL_ALLOWLIST.firstOrNull { it.modelId.equals(id, ignoreCase = true) }
                ?: DEFAULT_MODEL
        }
    }
}

