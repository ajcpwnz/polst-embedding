package com.polst.sdk.core.deviceid

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

public class EncryptedDeviceIdProvider(context: Context) : DeviceIdProvider {

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

    @Volatile
    private var cached: UUID? = null
    private val lock = Any()

    // First call may block for ~50ms due to EncryptedSharedPreferences disk I/O; subsequent calls are in-memory cached.
    override fun deviceId(): UUID {
        cached?.let { return it }
        return synchronized(lock) {
            cached?.let { return@synchronized it }
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            val uuid = if (existing != null) {
                UUID.fromString(existing)
            } else {
                val generated = UUID.randomUUID()
                prefs.edit().putString(KEY_DEVICE_ID, generated.toString()).apply()
                generated
            }
            cached = uuid
            uuid
        }
    }

    internal companion object {
        internal const val FILE_NAME: String = "polst-tokens"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
