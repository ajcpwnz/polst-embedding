package com.polst.sdk.network.dto

import com.polst.sdk.Brand
import com.polst.sdk.BrandRef
import com.polst.sdk.Campaign
import com.polst.sdk.CampaignStep
import com.polst.sdk.PageResult
import com.polst.sdk.Polst
import com.polst.sdk.PolstMedia
import com.polst.sdk.PolstOption
import com.polst.sdk.PolstThemingHints
import com.polst.sdk.Vote

internal fun PolstDto.toModel(): Polst =
    Polst(
        shortId = shortId,
        question = title,
        options = listOf(
            PolstOption(id = "A", label = optionA.label.orEmpty(), mediaUrl = optionA.imageUrl),
            PolstOption(id = "B", label = optionB.label.orEmpty(), mediaUrl = optionB.imageUrl),
        ),
        tallies = tallies
            ?.let { mapOf("A" to it.optionA.toLong(), "B" to it.optionB.toLong()) }
            ?: emptyMap(),
        brand = brand.toRef(),
        themingHints = null,
        media = null,
        createdAt = createdAt,
        version = 1,
    )

internal fun BrandSummaryDto.toRef(): BrandRef =
    BrandRef(
        slug = slug,
        displayName = name,
        logoUrl = avatarUrl,
    )

internal fun BrandDto.toModel(): Brand =
    Brand(
        id = slug,
        slug = slug,
        displayName = name,
        logoUrl = avatarUrl,
        accent = null,
    )

internal fun CampaignDto.toModel(): Campaign =
    Campaign(
        id = id,
        title = title,
        steps = steps.map { it.toModel() },
        brand = brand.toRef(),
        createdAt = createdAt,
    )

internal fun CampaignStepDto.toModel(): CampaignStep =
    CampaignStep(
        index = index,
        polst = polst.toModel(),
    )

internal fun BrandFeedPageDto.toPageResult(): PageResult<Polst> =
    PageResult(
        items = items.map { it.toModel() },
        nextCursor = nextCursor,
        hasMore = hasMore,
    )

internal fun Vote.toRequestBody(): VoteRequestDto = VoteRequestDto(option = optionId)

@Suppress("unused")
internal fun PolstMedia.ignored(): Nothing = error("PolstMedia is not currently produced by the API; kept for public API stability.")

@Suppress("unused")
internal fun PolstThemingHints.ignored(): Nothing = error("PolstThemingHints is not currently produced by the API; kept for public API stability.")
