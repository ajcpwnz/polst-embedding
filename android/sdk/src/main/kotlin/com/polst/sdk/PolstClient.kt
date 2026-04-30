package com.polst.sdk

import android.content.Context
import com.polst.sdk.brand.Scope
import com.polst.sdk.core.deviceid.DeviceIdProvider
import com.polst.sdk.core.deviceid.EncryptedDeviceIdProvider
import com.polst.sdk.core.logging.NoOpPolstLogger
import com.polst.sdk.core.logging.PolstLogger
import com.polst.sdk.core.tokens.EncryptedSharedPreferencesTokenStore
import com.polst.sdk.core.tokens.TokenStore
import com.polst.sdk.network.HttpUrlConnectionTransport
import com.polst.sdk.network.RestClient
import com.polst.sdk.offline.DefaultConnectivityObserver
import com.polst.sdk.offline.OfflineCache
import com.polst.sdk.offline.replay.ReplayDao
import com.polst.sdk.offline.replay.ReplayDatabase
import com.polst.sdk.offline.replay.ReplayScheduler
import kotlin.jvm.JvmOverloads

/**
 * Entry point for the Polst SDK.
 *
 * Phase 2 scope: this class constructs the low-level wiring (environment,
 * device-id provider, token store, REST client) and exposes the configured
 * collaborators to the rest of the SDK. Sub-client accessors (`polsts`,
 * `campaigns`, `brands`, `brand`) are intentionally absent and are added in
 * their respective story phases.
 *
 * Hosts typically instantiate one [PolstClient] at application startup, pass
 * it to [installDefault], and thereafter read it via [default] or
 * `LocalPolstClient` in Compose.
 */
public class PolstClient internal constructor(
    public val environment: Environment,
    public val deviceIdProvider: DeviceIdProvider,
    public val tokenStore: TokenStore,
    public val apiKeyId: String?,
    public val apiKeySecret: String?,
    public val scopes: Set<Scope>,
    public val logger: PolstLogger,
    internal val offlineCache: OfflineCache?,
    internal val replayDao: ReplayDao?,
    internal val replayScheduler: ReplayScheduler?,
) {

    @JvmOverloads
    public constructor(
        environment: Environment = Environment.Production,
        deviceIdProvider: DeviceIdProvider,
        tokenStore: TokenStore,
        apiKeyId: String? = null,
        apiKeySecret: String? = null,
        scopes: Set<Scope> = emptySet(),
        logger: PolstLogger = NoOpPolstLogger,
    ) : this(
        environment = environment,
        deviceIdProvider = deviceIdProvider,
        tokenStore = tokenStore,
        apiKeyId = apiKeyId,
        apiKeySecret = apiKeySecret,
        scopes = scopes,
        logger = logger,
        offlineCache = null,
        replayDao = null,
        replayScheduler = null,
    )

    /** Shared REST client used by every sub-client inside the SDK. */
    internal val rest: RestClient = RestClient(
        transport = HttpUrlConnectionTransport(),
        environment = environment,
        deviceIdProvider = deviceIdProvider,
        tokenStore = tokenStore,
        logger = logger,
    )

    init {
        replayScheduler?.start()
    }

    /** Public, unauthenticated single-Polst access and vote submission (US1). */
    public val polsts: com.polst.sdk.clients.PolstsClient by lazy {
        com.polst.sdk.clients.PolstsClient(
            rest = rest,
            environment = environment,
            cache = offlineCache,
            replayDao = replayDao,
            replayScheduler = replayScheduler,
        )
    }

    // TODO(US-campaigns): expose `campaigns` sub-client in the Campaigns story phase.
    // TODO(US-brands): expose `brands` sub-client in the Brands story phase.
    // TODO(US-brand): expose `brand` sub-client in the Brand story phase.

    public companion object {
        private var defaultInstance: PolstClient? = null

        /**
         * Installs a global default [PolstClient] reachable via [default]. Call
         * this once at app startup (typically from `Application.onCreate`).
         */
        public fun installDefault(client: PolstClient) {
            defaultInstance = client
        }

        /**
         * Returns the globally installed [PolstClient].
         *
         * @throws IllegalStateException if [installDefault] has not been called.
         */
        public val default: PolstClient
            get() = defaultInstance
                ?: error(
                    "PolstClient.installDefault(...) was not called. " +
                        "Call it once at app startup or pass a client explicitly.",
                )

        /**
         * Convenience factory that wires up an [EncryptedDeviceIdProvider] and
         * [EncryptedSharedPreferencesTokenStore] bound to the application
         * context.
         */
        public fun forContext(
            context: Context,
            environment: Environment = Environment.Production,
        ): PolstClient {
            val app = context.applicationContext
            val cache = OfflineCache(app)
            val dao = ReplayDatabase.getInstance(app).replayDao()
            val scheduler = ReplayScheduler(app, DefaultConnectivityObserver(app))
            return PolstClient(
                environment = environment,
                deviceIdProvider = EncryptedDeviceIdProvider(app),
                tokenStore = EncryptedSharedPreferencesTokenStore(app),
                apiKeyId = null,
                apiKeySecret = null,
                scopes = emptySet(),
                logger = NoOpPolstLogger,
                offlineCache = cache,
                replayDao = dao,
                replayScheduler = scheduler,
            )
        }
    }
}
