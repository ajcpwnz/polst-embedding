package com.polst.sdk.core.deviceid

import java.util.UUID

public fun interface DeviceIdProvider {
    public fun deviceId(): UUID
}
