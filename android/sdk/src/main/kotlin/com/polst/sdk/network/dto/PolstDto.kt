package com.polst.sdk.network.dto

import kotlinx.serialization.Serializable

/**
 * Every REST response is wrapped in a `data` envelope by the server.
 * Mirrors iOS `APIEnvelope<Item>` (`polst-ios/Sources/PolstSDK/Network/DTOs/DTOs.swift`).
 */
@Serializable
internal data class ApiEnvelope<T>(val data: T)

@Serializable
internal data class PolstDto(
    val shortId: String,
    val title: String,
    val optionA: PolstOptionDto,
    val optionB: PolstOptionDto,
    val tallies: PolstTallyDto? = null,
    val brand: BrandSummaryDto,
    @Serializable(with = InstantSerializer::class)
    val createdAt: java.time.Instant,
    val status: String? = null,
)

@Serializable
internal data class PolstOptionDto(
    val label: String? = null,
    val imageUrl: String? = null,
)

@Serializable
internal data class PolstTallyDto(
    val optionA: Int,
    val optionB: Int,
    val total: Int,
)
