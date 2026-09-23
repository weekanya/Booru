package com.booru.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

class SecureCredentialsStorage(context: Context) {

    private val memoryCache = ConcurrentHashMap<String, String>()

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

    private fun getCachedString(key: String): String {
        return memoryCache.computeIfAbsent(key) {
            prefs?.getString(key, "") ?: ""
        }
    }

    private fun putCachedString(key: String, value: String): Boolean {
        val trimmed = value.trim()
        val success = prefs?.edit()?.putString(key, trimmed)?.commit() ?: false
        if (success) {
            memoryCache[key] = trimmed
        }
        return success
    }

    private fun removeCachedString(key: String): Boolean {
        val success = prefs?.edit()?.remove(key)?.commit() ?: false
        if (success) {
            memoryCache.remove(key)
        }
        return success
    }

    fun getRule34UserId(): String = getCachedString(KEY_R34_USER_ID)
    fun setRule34UserId(value: String): Boolean = putCachedString(KEY_R34_USER_ID, value)

    fun getRule34ApiKey(): String = getCachedString(KEY_R34_API_KEY)
    fun setRule34ApiKey(value: String): Boolean = putCachedString(KEY_R34_API_KEY, value)

    fun getGelbooruUserId(): String = getCachedString(KEY_GEL_USER_ID)
    fun setGelbooruUserId(value: String): Boolean = putCachedString(KEY_GEL_USER_ID, value)

    fun getGelbooruApiKey(): String = getCachedString(KEY_GEL_API_KEY)
    fun setGelbooruApiKey(value: String): Boolean = putCachedString(KEY_GEL_API_KEY, value)

    fun getCustomApiKey(sourceId: String): String = getCachedString(customApiKey(sourceId))
    fun setCustomApiKey(sourceId: String, value: String): Boolean = putCachedString(customApiKey(sourceId), value)
    fun removeCustomApiKey(sourceId: String): Boolean = removeCachedString(customApiKey(sourceId))

    fun getCustomUserId(sourceId: String): String = getCachedString(customUserId(sourceId))
    fun setCustomUserId(sourceId: String, value: String): Boolean = putCachedString(customUserId(sourceId), value)
    fun removeCustomUserId(sourceId: String): Boolean = removeCachedString(customUserId(sourceId))

    fun removeCustomCredentials(sourceId: String): Boolean {
        val k1 = customApiKey(sourceId)
        val k2 = customUserId(sourceId)
        val success = prefs?.edit()
            ?.remove(k1)
            ?.remove(k2)
            ?.commit() ?: false
        if (success) {
            memoryCache.remove(k1)
            memoryCache.remove(k2)
        }
        return success
    }
}

