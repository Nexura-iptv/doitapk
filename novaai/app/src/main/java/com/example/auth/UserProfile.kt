package com.example.auth

data class UserProfile(
    val id: String = "",
    val displayName: String = "Guest User",
    val email: String = "",
    val photoUrl: String? = null,
    val idToken: String? = null,
    val isSignedIn: Boolean = false
)

object AppCertificateInfo {
    const val PACKAGE_NAME = "com.aistudio.novaai.app"
    const val SHA1_FINGERPRINT = "DA:B2:68:6C:10:1A:98:0A:5C:22:41:9D:F1:4D:8E:2E:3C:A7:DB:96"
    const val SHA256_FINGERPRINT = "30:01:E1:B4:74:1B:16:C1:B5:F7:5D:56:DE:44:43:59:88:57:96:BC:7E:5E:87:21:2D:5E:D3:95:A4:66:F3:6B"
    const val APP_NAME = "NovaAI"
}
