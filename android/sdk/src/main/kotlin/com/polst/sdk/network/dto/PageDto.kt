package com.polst.sdk.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class BrandFeedPageDto(
    val items: List<PolstDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)
