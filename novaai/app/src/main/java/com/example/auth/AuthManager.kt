package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_ai_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(loadSavedUser())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private fun loadSavedUser(): UserProfile {
        val isSignedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        return if (isSignedIn) {
            UserProfile(
                id = prefs.getString(KEY_USER_ID, "") ?: "",
                displayName = prefs.getString(KEY_USER_NAME, "Nova Kullanıcısı") ?: "Nova Kullanıcısı",
                email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
                photoUrl = prefs.getString(KEY_USER_PHOTO, null),
                idToken = prefs.getString(KEY_ID_TOKEN, null),
                isSignedIn = true
            )
        } else {
            UserProfile()
        }
    }

    suspend fun signIn(username: String, email: String, password: String = ""): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            val cleanUsername = username.trim()
            val cleanEmail = email.trim()

            if (cleanUsername.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Kullanıcı adı boş bırakılamaz."))
            }
            if (cleanEmail.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("E-posta adresi boş bırakılamaz."))
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                return@withContext Result.failure(IllegalArgumentException("Geçerli bir e-posta adresi giriniz."))
            }

            val user = UserProfile(
                id = "user_${System.currentTimeMillis()}",
                displayName = cleanUsername,
                email = cleanEmail,
                photoUrl = null,
                idToken = "token_${System.currentTimeMillis()}",
                isSignedIn = true
            )

            saveUser(user)
            Result.success(user)
        }

    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .remove(KEY_ID_TOKEN)
            .apply()
        _currentUser.value = UserProfile()
    }

    private fun saveUser(user: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .putString(KEY_ID_TOKEN, user.idToken)
            .apply()
        _currentUser.value = user
    }

    companion object {
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_ID_TOKEN = "id_token"
    }
}
