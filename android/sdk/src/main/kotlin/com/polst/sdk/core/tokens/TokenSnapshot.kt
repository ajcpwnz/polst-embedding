package com.polst.sdk.core.tokens

import com.polst.sdk.brand.Scope
import java.time.Instant

public data class TokenSnapshot(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val scopesGranted: Set<Scope>,
)
