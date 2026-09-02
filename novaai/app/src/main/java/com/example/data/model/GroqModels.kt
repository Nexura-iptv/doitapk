package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<GroqMessagePayload>,
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int = 2048,
    @Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GroqMessagePayload(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: Any // Can be String or List<GroqContentPart>
)

@JsonClass(generateAdapter = true)
data class GroqContentPart(
    @Json(name = "type") val type: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "image_url") val imageUrl: GroqImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class GroqImageUrl(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<GroqChoice> = emptyList(),
    @Json(name = "usage") val usage: GroqUsage? = null,
    @Json(name = "model") val model: String? = null
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: GroqResponseMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GroqResponseMessage(
    @Json(name = "role") val role: String = "assistant",
    @Json(name = "content") val content: String = ""
)

@JsonClass(generateAdapter = true)
data class GroqUsage(
    @Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @Json(name = "completion_tokens") val completionTokens: Int? = null,
    @Json(name = "total_tokens") val totalTokens: Int? = null
)

data class AiModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val isVisionSupported: Boolean = false,
    val provider: String = "Groq"
)

object AvailableModels {
    val QWEN_3_6_27B = AiModelInfo(
        id = "qwen/qwen3.6-27b",
        displayName = "Qwen 3.6 27B",
        description = "Gelişmiş akıl yürütme, kodlama ve Türkçe anlama yeteneğine sahip ana Qwen modeli",
        isVisionSupported = true,
        provider = "Groq"
    )
    val LLAMA_3_2_11B_VISION = AiModelInfo(
        id = "llama-3.2-11b-vision-preview",
        displayName = "Llama 3.2 11B Vision",
        description = "Yüksek hızlı görsel ve doküman inceleme modeli",
        isVisionSupported = true,
        provider = "Groq"
    )
    val OPENAI_GPT_OSS_120B = AiModelInfo(
        id = "openai/gpt-oss-120b",
        displayName = "OpenAI GPT OSS 120B",
        description = "OpenAI açık ağırlıklı 120B yüksek zeka modeli",
        isVisionSupported = false,
        provider = "Groq"
    )
    val LLAMA_3_3_70B = AiModelInfo(
        id = "llama-3.3-70b-versatile",
        displayName = "Llama 3.3 70B Versatile",
        description = "Kapsamlı mantıksal problem çözme ve çok yönlü asistan",
        isVisionSupported = false,
        provider = "Groq"
    )
    val DEEPSEEK_R1_DISTILL_70B = AiModelInfo(
        id = "deepseek-r1-distill-llama-70b",
        displayName = "DeepSeek R1 Distill 70B",
        description = "Derin mantıksal akıl yürütme ve adım adım analiz",
        isVisionSupported = false,
        provider = "Groq"
    )
    val LLAMA_3_2_90B_VISION = AiModelInfo(
        id = "llama-3.2-90b-vision-preview",
        displayName = "Llama 3.2 90B Vision",
        description = "Groq üzerindeki amiral gemisi çok modlu görsel yapay zeka",
        isVisionSupported = true,
        provider = "Groq"
    )

    val allModels = listOf(
        QWEN_3_6_27B,
        LLAMA_3_2_11B_VISION,
        OPENAI_GPT_OSS_120B,
        LLAMA_3_3_70B,
        DEEPSEEK_R1_DISTILL_70B,
        LLAMA_3_2_90B_VISION
    )

    val defaultModel = QWEN_3_6_27B
}
