package com.polst.sdk.network.dto

import kotlinx.serialization.Serializable

/**
 * POST /polsts/{shortId}/votes request body.
 * The API accepts `option: "A"` or `option: "B"` (not a free-form option id).
 * Mirrors iOS `VoteRequestDTO`.
 */
@Serializable
internal data class VoteRequestDto(val option: String)

/**
 * POST /polsts/{shortId}/votes response (inside the `data` envelope).
 * Server returns updated tallies only, not a full Polst. Callers refetch
 * via `get` if they need the full card refreshed.
 * Mirrors iOS `VoteResponseDTO`.
 */
@Serializable
internal data class VoteResponseDto(
    val tallies: PolstTallyDto,
    val isRevote: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val votedAt: java.time.Instant,
)
