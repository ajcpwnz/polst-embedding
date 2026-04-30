package com.polst.sdk.clients

import com.polst.sdk.Environment
import com.polst.sdk.Polst
import com.polst.sdk.Vote
import com.polst.sdk.VoteState
import com.polst.sdk.network.PolstApiError
import com.polst.sdk.network.RestClient
import com.polst.sdk.network.dto.ApiEnvelope
import com.polst.sdk.network.dto.PolstDto
import com.polst.sdk.network.dto.VoteRequestDto
import com.polst.sdk.network.dto.VoteResponseDto
import com.polst.sdk.network.dto.toModel
import com.polst.sdk.offline.CacheFreshness
import com.polst.sdk.offline.OfflineCache
import com.polst.sdk.offline.replay.ReplayDao
import com.polst.sdk.offline.replay.ReplayEntity
import com.polst.sdk.offline.replay.ReplayScheduler
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json

public class PolstsClient internal constructor(
    private val rest: RestClient,
    private val environment: Environment? = null,
    private val cache: OfflineCache? = null,
    private val replayDao: ReplayDao? = null,
    private val replayScheduler: ReplayScheduler? = null,
    private val json: Json = defaultJson,
) {

    public suspend fun get(shortId: String): Polst = getWithFreshness(shortId).polst

    internal suspend fun getWithFreshness(shortId: String): FetchResult {
        return try {
            val dto = rest.get<ApiEnvelope<PolstDto>>("/polsts/$shortId").data
            cache?.writePolst(dto)
            FetchResult(dto.toModel(), CacheFreshness.Fresh)
        } catch (networkErr: PolstApiError.Network) {
            val cached = cache?.readPolst(shortId)
            if (cached != null) {
                FetchResult(cached.payload.toModel(), CacheFreshness.StaleOffline)
            } else {
                throw networkErr
            }
        } catch (serverErr: PolstApiError.ServerError) {
            val cached = cache?.readPolst(shortId)
            if (cached != null) {
                FetchResult(cached.payload.toModel(), CacheFreshness.StaleOffline)
            } else {
                throw serverErr
            }
        }
    }

    public suspend fun vote(shortId: String, optionId: String): Vote {
        val idempotencyKey: UUID = UUID.randomUUID()
        val castAt: Instant = Instant.now()
        val requestBody = VoteRequestDto(option = optionId)
        val bodyBytes = json.encodeToString(VoteRequestDto.serializer(), requestBody).toByteArray()

        val baseUrl = environment?.baseUrl ?: ""
        val endpointFullUrl = "$baseUrl/polsts/$shortId/votes"

        replayDao?.insert(
            ReplayEntity(
                idempotencyKey = idempotencyKey.toString(),
                endpoint = endpointFullUrl,
                method = "POST",
                body = bodyBytes,
                createdAt = System.currentTimeMillis(),
                lastAttemptAt = null,
                attemptCount = 0,
                lastErrorClass = null,
            ),
        )

        return try {
            rest.post<VoteRequestDto, ApiEnvelope<VoteResponseDto>>(
                path = "/polsts/$shortId/votes",
                body = requestBody,
                headers = mapOf("X-Polst-Idempotency-Key" to idempotencyKey.toString()),
            )
            replayDao?.deleteByKey(idempotencyKey.toString())
            Vote(
                polstShortId = shortId,
                optionId = optionId,
                idempotencyKey = idempotencyKey,
                castAt = castAt,
                state = VoteState.Acknowledged(Instant.now()),
            )
        } catch (networkErr: PolstApiError.Network) {
            replayScheduler?.enqueueNow()
            Vote(
                polstShortId = shortId,
                optionId = optionId,
                idempotencyKey = idempotencyKey,
                castAt = castAt,
                state = VoteState.Pending,
            )
        }
    }

    internal data class FetchResult(val polst: Polst, val freshness: CacheFreshness)

    internal companion object {
        internal val defaultJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    }
}
