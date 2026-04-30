package com.polst.sdk.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CampaignDto(
    val id: String,
    val title: String,
    val steps: List<CampaignStepDto>,
    val brand: BrandSummaryDto,
    @Serializable(with = InstantSerializer::class)
    val createdAt: java.time.Instant,
)

@Serializable
internal data class CampaignStepDto(
    val index: Int,
    val polst: PolstDto,
)
