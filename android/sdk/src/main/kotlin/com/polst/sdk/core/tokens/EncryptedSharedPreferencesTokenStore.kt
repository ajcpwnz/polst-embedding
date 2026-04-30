package com.polst.sdk.core.tokens

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.polst.sdk.brand.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

public class EncryptedSharedPreferencesTokenStore(context: Context) : TokenStore {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun read(): TokenSnapshot? = withContext(Dispatchers.IO) {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return@withContext null
        if (!prefs.contains(KEY_EXPIRES_AT)) return@withContext null
        val expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, Long.MIN_VALUE)
        if (expiresAtMillis == Long.MIN_VALUE) return@withContext null
        val scopesRaw = prefs.getString(KEY_SCOPES_GRANTED, null) ?: return@withContext null
        val scopes = parseScopes(scopesRaw)
        TokenSnapshot(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.ofEpochMilli(expiresAtMillis),
            scopesGranted = scopes,
        )
    }

    override suspend fun write(snapshot: TokenSnapshot): Unit = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, snapshot.accessToken)
            .putString(KEY_REFRESH_TOKEN, snapshot.refreshToken)
            .putLong(KEY_EXPIRES_AT, snapshot.expiresAt.toEpochMilli())
            .putString(KEY_SCOPES_GRANTED, serializeScopes(snapshot.scopesGranted))
            .apply()
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun serializeScopes(scopes: Set<Scope>): String =
        scopes.joinToString(separator = ",") { it.wireValue }

    private fun parseScopes(raw: String): Set<Scope> {
        if (raw.isEmpty()) return emptySet()
        return raw.split(",")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { Scope.fromWireValue(it) }
            .toSet()
    }

    internal companion object {
        internal const val FILE_NAME: String = "polst-tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_SCOPES_GRANTED = "scopes_granted"
    }
}
