package com.polst.sdk

import java.time.Instant

public data class Campaign(
    val id: String,
    val title: String,
    val steps: List<CampaignStep>,
    val brand: BrandRef,
    val createdAt: Instant,
)

public data class CampaignStep(
    val index: Int,
    val polst: Polst,
)
