package com.example.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.GroqChatResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GroqClient(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(GroqApiService::class.java)

    /**
     * Get the Groq API key from BuildConfig (injected via .env by Secrets Gradle Plugin)
     */
    fun getApiKey(): String {
        return try {
            BuildConfig.GROQ_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Send chat completion request supporting both text and image attachments
     */
    suspend fun sendChatMessage(
        modelId: String,
        prompt: String,
        imageUri: Uri? = null,
        history: List<Pair<String, String>> = emptyList() // role to content
    ): Result<GroqChatResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank() || apiKey == "MY_GROQ_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("Groq API Key is not configured. Please check your .env file or Secrets panel.")
                )
            }

            // Determine if vision model should be used
            val effectiveModel = if (imageUri != null) {
                if (modelId.contains("vision", ignoreCase = true)) {
                    modelId
                } else {
                    "llama-3.2-11b-vision-preview"
                }
            } else {
                modelId
            }

            val requestJson = JSONObject()
            requestJson.put("model", effectiveModel)
            requestJson.put("temperature", 0.7)
            requestJson.put("max_tokens", 2048)

            val messagesArray = JSONArray()

            // System prompt
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", "You are NovaAI, a highly capable, fast, and helpful AI assistant powered by Groq. Provide clear, accurate, and insightful responses in the user's language (Turkish when queried in Turkish). Format neatly with markdown. Do not wrap thoughts in visible reasoning tags if avoidable.")
            messagesArray.put(systemMsg)

            // Conversation history
            history.takeLast(8).forEach { (role, text) ->
                if (text.isNotBlank()) {
                    val histMsg = JSONObject()
                    histMsg.put("role", role)
                    histMsg.put("content", text)
                    messagesArray.put(histMsg)
                }
            }

            // Current user message
            val userMsg = JSONObject()
            userMsg.put("role", "user")

            if (imageUri != null) {
                // Multimodal format (OpenAI compatible on Groq)
                val contentArray = JSONArray()

                val textPart = JSONObject()
                textPart.put("type", "text")
                textPart.put("text", prompt.ifBlank { "Bu görseli detaylı bir şekilde analiz et ve açıkla." })
                contentArray.put(textPart)

                val base64Image = uriToBase64(imageUri)
                if (base64Image != null) {
                    val imagePart = JSONObject()
                    imagePart.put("type", "image_url")
                    val imageUrlObj = JSONObject()
                    imageUrlObj.put("url", "data:image/jpeg;base64,$base64Image")
                    imagePart.put("image_url", imageUrlObj)
                    contentArray.put(imagePart)
                }

                userMsg.put("content", contentArray)
            } else {
                userMsg.put("content", prompt)
            }

            messagesArray.put(userMsg)

            requestJson.put("messages", messagesArray)

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val response = apiService.createChatCompletion("Bearer $apiKey", requestBody)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body received from Groq API"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("Groq API Error (${response.code()}): $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Resize if too large to conserve bandwidth & fit API constraints
                val maxDim = 1024
                val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else {
                    bitmap
                }

                val outputStream = ByteArrayOutputStream()
                scale.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val byteArray = outputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
