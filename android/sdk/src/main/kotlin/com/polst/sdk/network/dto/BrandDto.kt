package com.polst.sdk.network.dto

import kotlinx.serialization.Serializable

/**
 * Brand summary embedded in Polst / Campaign responses.
 * Mirrors iOS `BrandSummaryDTO`.
 */
@Serializable
internal data class BrandSummaryDto(
    val slug: String,
    val name: String,
    val avatarUrl: String? = null,
)

/**
 * Full brand profile returned by `GET /brands/{slug}`.
 * Kept as a separate DTO because the full shape may diverge from the summary.
 */
@Serializable
internal data class BrandDto(
    val slug: String,
    val name: String,
    val avatarUrl: String? = null,
)
