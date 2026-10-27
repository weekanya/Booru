package com.booru.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class SecureCredentialsStorage(context: Context) {

    private class Store(val prefs: SharedPreferences, val cipher: KeystoreCipher)

    private val memoryCache = ConcurrentHashMap<String, String>()

    private val appContext = context.applicationContext

    private val store: Store? by lazy { openStore() }

    private fun openStore(): Store? {
        val cipher = KeystoreCipher(KEY_ALIAS)
        var lastError: Exception? = null
        repeat(2) {
            try {
                cipher.ensureKey()
                val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                migrateLegacy(prefs, cipher)
                runCatching { appContext.deleteSharedPreferences(FALLBACK_PREFS_NAME) }
                return Store(prefs, cipher)
            } catch (e: Exception) {
                lastError = e
            }
        }
        Log.e(TAG, "Keystore unavailable: ${lastError?.javaClass?.simpleName}")
        return null
    }

    private fun migrateLegacy(prefs: SharedPreferences, cipher: KeystoreCipher) {
        val legacyFile = java.io.File(appContext.dataDir, "shared_prefs/$LEGACY_PREFS_NAME.xml")
        if (!legacyFile.exists()) return
        val legacy = try {
            openLegacy()
        } catch (e: Exception) {
            val attempts = prefs.getInt(KEY_LEGACY_ATTEMPTS, 0) + 1
            Log.w(TAG, "Legacy credentials unreadable (attempt $attempts): ${e.javaClass.simpleName}")
            if (attempts >= MAX_LEGACY_ATTEMPTS) {
                dropLegacy()
                prefs.edit().remove(KEY_LEGACY_ATTEMPTS).commit()
            } else {
                prefs.edit().putInt(KEY_LEGACY_ATTEMPTS, attempts).commit()
            }
            return
        }
        val editor = prefs.edit()
        var migrated = 0
        for ((key, value) in legacy.all) {
            if (value !is String || value.isBlank() || prefs.contains(key)) continue
            editor.putString(key, cipher.encrypt(value.trim(), key))
            migrated++
        }
        editor.remove(KEY_LEGACY_ATTEMPTS)
        if (editor.commit()) {
            dropLegacy()
            Log.i(TAG, "Moved $migrated credentials to keystore storage")
        }
    }

    @Suppress("DEPRECATION")
    private fun openLegacy(): SharedPreferences {
        val masterKey = androidx.security.crypto.MasterKey.Builder(appContext)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        return androidx.security.crypto.EncryptedSharedPreferences.create(
            appContext,
            LEGACY_PREFS_NAME,
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun dropLegacy() {
        runCatching { appContext.deleteSharedPreferences(LEGACY_PREFS_NAME) }
        KeystoreCipher.deleteKey(LEGACY_MASTER_KEY_ALIAS)
    }

    companion object {
        private const val TAG = "SecureCredentials"
        private const val PREFS_NAME = "booru_credentials_v2"
        private const val KEY_ALIAS = "booru_credentials_key"
        private const val LEGACY_PREFS_NAME = "booru_secure_credentials"
        private const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val FALLBACK_PREFS_NAME = "booru_fallback_credentials"
        private const val KEY_LEGACY_ATTEMPTS = "legacy_migration_attempts"
        private const val MAX_LEGACY_ATTEMPTS = 3
        private const val KEY_R34_USER_ID = "sec_r34_uid"
        private const val KEY_R34_API_KEY = "sec_r34_key"
        private const val KEY_GEL_USER_ID = "sec_gel_uid"
        private const val KEY_GEL_API_KEY = "sec_gel_key"

        private fun customApiKey(id: String) = "custom_${id}_api_key"
        private fun customUserId(id: String) = "custom_${id}_user_id"
    }

    val isSecureStorageAvailable: Boolean
        get() = store != null

    private fun readDecrypted(key: String): String {
        val current = store ?: return ""
        val encoded = current.prefs.getString(key, null) ?: return ""
        return try {
            current.cipher.decrypt(encoded, key)
        } catch (e: Exception) {
            Log.w(TAG, "Dropping unreadable credential: ${e.javaClass.simpleName}")
            current.prefs.edit().remove(key).commit()
            ""
        }
    }

    private fun getCachedString(key: String): String {
        return memoryCache.computeIfAbsent(key) { readDecrypted(it) }
    }

    private fun putCachedString(key: String, value: String): Boolean {
        val current = store ?: return false
        val trimmed = value.trim()
        val success = try {
            val editor = current.prefs.edit()
            if (trimmed.isEmpty()) editor.remove(key) else editor.putString(key, current.cipher.encrypt(trimmed, key))
            editor.commit()
        } catch (e: Exception) {
            Log.w(TAG, "Credential write failed: ${e.javaClass.simpleName}")
            false
        }
        if (success) {
            memoryCache[key] = trimmed
        }
        return success
    }

    private fun removeCachedString(key: String): Boolean {
        val success = store?.prefs?.edit()?.remove(key)?.commit() ?: false
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
        val success = store?.prefs?.edit()
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
