package com.polst.sdk.network

import com.polst.sdk.brand.Scope
import kotlin.time.Duration

public sealed class PolstApiError(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    public data class Network(val underlying: Throwable) : PolstApiError("Network error", underlying)

    public data class HttpStatus(val code: Int, val body: ByteArray?) : PolstApiError("HTTP $code")

    public data class Decoding(val underlying: Throwable) : PolstApiError("Decoding error", underlying)

    public data object AuthenticationMissing : PolstApiError("No credentials configured for brand surface")

    public data class AuthenticationFailed(val reason: String?) : PolstApiError(reason ?: "Authentication failed")

    public data object AuthenticationExpired : PolstApiError("Refresh failed; re-authenticate")

    public data class AuthorizationInsufficient(val requiredScope: Scope) :
        PolstApiError("Missing scope: ${requiredScope.wireValue}")

    public data class RateLimited(val retryAfter: Duration?) :
        PolstApiError("Rate limited (retry-after = $retryAfter)")

    public data class Validation(val fieldErrors: Map<String, String>) :
        PolstApiError("Validation failed: $fieldErrors")

    public data class NotFound(val resource: String, val id: String) :
        PolstApiError("$resource $id not found")

    public data class ServerError(val code: Int, val body: ByteArray?) :
        PolstApiError("Server error $code")
}
