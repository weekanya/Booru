package com.booru.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCredentialsStorage(context: Context) {

    private val prefs: SharedPreferences? = try {
        try {
            context.deleteSharedPreferences("booru_fallback_credentials")
        } catch (_: Exception) {}

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "booru_secure_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SecureCredentials", "Hardware-backed keystore unavailable: ${e.message}")
        null
    }

    companion object {
        private const val KEY_R34_USER_ID = "sec_r34_uid"
        private const val KEY_R34_API_KEY = "sec_r34_key"
        private const val KEY_GEL_USER_ID = "sec_gel_uid"
        private const val KEY_GEL_API_KEY = "sec_gel_key"

        private fun customApiKey(id: String) = "custom_${id}_api_key"
        private fun customUserId(id: String) = "custom_${id}_user_id"
    }

    val isSecureStorageAvailable: Boolean
        get() = prefs != null

    fun getRule34UserId(): String = prefs?.getString(KEY_R34_USER_ID, "") ?: ""
    fun setRule34UserId(value: String) {
        prefs?.edit()?.putString(KEY_R34_USER_ID, value.trim())?.apply()
    }

    fun getRule34ApiKey(): String = prefs?.getString(KEY_R34_API_KEY, "") ?: ""
    fun setRule34ApiKey(value: String) {
        prefs?.edit()?.putString(KEY_R34_API_KEY, value.trim())?.apply()
    }

    fun getGelbooruUserId(): String = prefs?.getString(KEY_GEL_USER_ID, "") ?: ""
    fun setGelbooruUserId(value: String) {
        prefs?.edit()?.putString(KEY_GEL_USER_ID, value.trim())?.apply()
    }

    fun getGelbooruApiKey(): String = prefs?.getString(KEY_GEL_API_KEY, "") ?: ""
    fun setGelbooruApiKey(value: String) {
        prefs?.edit()?.putString(KEY_GEL_API_KEY, value.trim())?.apply()
    }

    fun getCustomApiKey(sourceId: String): String = prefs?.getString(customApiKey(sourceId), "") ?: ""
    fun setCustomApiKey(sourceId: String, value: String) {
        prefs?.edit()?.putString(customApiKey(sourceId), value.trim())?.apply()
    }
    fun removeCustomApiKey(sourceId: String) {
        prefs?.edit()?.remove(customApiKey(sourceId))?.apply()
    }

    fun getCustomUserId(sourceId: String): String = prefs?.getString(customUserId(sourceId), "") ?: ""
    fun setCustomUserId(sourceId: String, value: String) {
        prefs?.edit()?.putString(customUserId(sourceId), value.trim())?.apply()
    }
    fun removeCustomUserId(sourceId: String) {
        prefs?.edit()?.remove(customUserId(sourceId))?.apply()
    }

    fun removeCustomCredentials(sourceId: String) {
        prefs?.edit()
            ?.remove(customApiKey(sourceId))
            ?.remove(customUserId(sourceId))
            ?.apply()
    }
}

