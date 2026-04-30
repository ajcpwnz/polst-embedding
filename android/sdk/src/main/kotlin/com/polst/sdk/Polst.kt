package com.polst.sdk

import java.time.Instant

public data class Polst(
    val shortId: String,
    val question: String,
    val options: List<PolstOption>,
    val tallies: Map<String, Long>,
    val brand: BrandRef,
    val themingHints: PolstThemingHints?,
    val media: PolstMedia?,
    val createdAt: Instant,
    val version: Int,
) {
    init {
        require(shortId.isNotBlank()) { "shortId must not be blank" }
        require(options.size in 2..10) { "options.size must be in 2..10, was ${options.size}" }
    }
}

public data class PolstOption(
    val id: String,
    val label: String,
    val mediaUrl: String?,
)

public data class PolstMedia(
    val heroUrl: String?,
    val videoUrl: String?,
)

public data class PolstThemingHints(
    val suggestedAccent: String? = null,
    val preferDark: Boolean? = null,
)
