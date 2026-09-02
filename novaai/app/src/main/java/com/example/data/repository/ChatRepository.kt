package com.example.data.repository

import android.net.Uri
import com.example.data.local.ChatMessage
import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatSession
import com.example.data.local.ChatSessionDao
import com.example.data.network.GroqClient
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao,
    private val groqClient: GroqClient
) {

    fun getAllSessions(): Flow<List<ChatSession>> = sessionDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> =
        messageDao.getMessagesForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): ChatSession? =
        sessionDao.getSessionById(sessionId)

    suspend fun createNewSession(
        title: String = "Yeni Sohbet",
        modelId: String = "qwen/qwen3.6-27b"
    ): Long {
        val session = ChatSession(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelId = modelId,
            lastMessage = ""
        )
        return sessionDao.insertSession(session)
    }

    suspend fun deleteSession(sessionId: Long) {
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun clearAllHistory() {
        messageDao.deleteAllMessages()
        sessionDao.deleteAllSessions()
    }

    suspend fun updateSessionTitle(sessionId: Long, title: String) {
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            sessionDao.updateSession(session.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun sendMessage(
        sessionId: Long,
        modelId: String,
        userPrompt: String,
        imageUri: Uri? = null
    ): Result<ChatMessage> {
        // 1. Insert User Message
        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            imageUri = imageUri?.toString(),
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS",
            modelUsed = modelId
        )
        messageDao.insertMessage(userMessage)

        // 2. Insert Assistant Placeholder (Loading)
        val assistantPlaceholder = ChatMessage(
            sessionId = sessionId,
            role = "assistant",
            content = "",
            timestamp = System.currentTimeMillis(),
            status = "LOADING",
            modelUsed = modelId
        )
        val assistantMsgId = messageDao.insertMessage(assistantPlaceholder)

        // 3. Fetch past messages for context
        val recentMessages = messageDao.getMessagesListForSession(sessionId)
        val historyPairs = recentMessages
            .filter { it.status == "SUCCESS" && it.id != assistantMsgId && it.id != userMessage.id }
            .takeLast(10)
            .map { it.role to it.content }

        // Update session's last message & title if it's the first message
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            val shortTitle = if (session.title == "New Conversation" || session.title == "Yeni Sohbet") {
                val cleanPrompt = userPrompt.ifBlank { "Görsel Analizi" }
                if (cleanPrompt.length > 30) cleanPrompt.take(30) + "…" else cleanPrompt
            } else {
                session.title
            }
            sessionDao.updateSession(
                session.copy(
                    title = shortTitle,
                    updatedAt = System.currentTimeMillis(),
                    lastMessage = userPrompt.ifBlank { "Görsel Eklendi" },
                    modelId = modelId
                )
            )
        }

        // 4. Call Groq API
        val result = groqClient.sendChatMessage(
            modelId = modelId,
            prompt = userPrompt,
            imageUri = imageUri,
            history = historyPairs
        )

        return if (result.isSuccess) {
            val response = result.getOrNull()
            val rawContent = response?.choices?.firstOrNull()?.message?.content ?: "Yanıt alınamadı."
            val cleanContent = stripThinkingTags(rawContent)

            val updatedMessage = assistantPlaceholder.copy(
                id = assistantMsgId,
                content = cleanContent,
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                modelUsed = response?.model ?: modelId
            )
            messageDao.updateMessage(updatedMessage)

            // Update session last message
            if (session != null) {
                sessionDao.updateSession(
                    session.copy(
                        updatedAt = System.currentTimeMillis(),
                        lastMessage = if (cleanContent.length > 50) cleanContent.take(50) + "…" else cleanContent
                    )
                )
            }

            Result.success(updatedMessage)
        } else {
            val error = result.exceptionOrNull()
            val errorMessage = error?.message ?: "Bağlantı hatası oluştu"
            val failedMessage = assistantPlaceholder.copy(
                id = assistantMsgId,
                content = "Hata: $errorMessage",
                status = "ERROR",
                timestamp = System.currentTimeMillis()
            )
            messageDao.updateMessage(failedMessage)
            Result.failure(error ?: Exception(errorMessage))
        }
    }

    /**
     * Filters out reasoning and thinking process tags so they are not displayed to the user
     */
    companion object {
        fun stripThinkingTags(raw: String): String {
            if (raw.isBlank()) return raw
            var cleaned = raw
            // 1. Remove complete <think>...</think> blocks (case insensitive, dotAll)
            cleaned = cleaned.replace(Regex("(?si)<think>.*?</think>"), "")
            // 2. Remove complete <thinking>...</thinking> blocks
            cleaned = cleaned.replace(Regex("(?si)<thinking>.*?</thinking>"), "")
            // 3. Remove complete [thinking]...[/thinking] blocks
            cleaned = cleaned.replace(Regex("(?si)\\[thinking\\].*?\\[/thinking\\]"), "")
            // 4. Remove complete <thought>...</thought> or <reasoning>...</reasoning>
            cleaned = cleaned.replace(Regex("(?si)<thought>.*?</thought>"), "")
            cleaned = cleaned.replace(Regex("(?si)<reasoning>.*?</reasoning>"), "")
            // 5. Remove unclosed trailing <think>... if any
            cleaned = cleaned.replace(Regex("(?si)<think>.*"), "")
            cleaned = cleaned.replace(Regex("(?si)<thinking>.*"), "")

            return cleaned.trim()
        }
    }
}
