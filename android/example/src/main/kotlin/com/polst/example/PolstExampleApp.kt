package com.polst.example

import android.app.Application
import com.polst.sdk.Environment
import com.polst.sdk.PolstClient

/**
 * Boots the in-process [MockBackend] and installs a [PolstClient] pointed at
 * it so every example screen can render and vote without any external backend.
 *
 * Production apps would install a [PolstClient] backed by `Environment.Production`
 * inside their own `Application.onCreate`.
 */
class PolstExampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // To smoke-test offline behaviour against canned data, swap the two lines below
        // for `MockBackend.start()` + `Environment.Custom(MockBackend.BASE_URL)`.
        val client = PolstClient.forContext(
            context = this,
            environment = Environment.Production,
        )
        PolstClient.installDefault(client)
    }
}
