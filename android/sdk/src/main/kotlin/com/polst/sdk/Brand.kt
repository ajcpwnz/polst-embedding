package com.polst.sdk

public data class Brand(
    val id: String,
    val slug: String,
    val displayName: String,
    val logoUrl: String?,
    val accent: String?,
)

public data class BrandRef(
    val slug: String,
    val displayName: String,
    val logoUrl: String?,
)
