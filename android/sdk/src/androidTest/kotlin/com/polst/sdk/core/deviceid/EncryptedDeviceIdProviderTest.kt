package com.polst.sdk.core.deviceid

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

public class EncryptedDeviceIdProviderTest {

    private lateinit var context: Context

    @Before
    public fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clearPrefs(context)
    }

    @Test
    public fun deviceId_isStableAcrossInstances() {
        val first = EncryptedDeviceIdProvider(context).deviceId()
        val second = EncryptedDeviceIdProvider(context).deviceId()
        assertEquals(first, second)
    }

    @Test
    public fun deviceId_regeneratesAfterClear() {
        val first = EncryptedDeviceIdProvider(context).deviceId()
        clearPrefs(context)
        val second = EncryptedDeviceIdProvider(context).deviceId()
        assertNotEquals(first, second)
    }

    private fun clearPrefs(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "polst-tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.edit().clear().commit()
    }
}
