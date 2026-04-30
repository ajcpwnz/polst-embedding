package com.polst.sdk

public sealed class Environment {
    public abstract val baseUrl: String

    public data object Production : Environment() {
        override val baseUrl: String = "https://canary-api.polst.app/api/rest/v1"
    }

    public data object Staging : Environment() {
        override val baseUrl: String = "https://staging-api.polst.app/v1"
    }

    public data class Custom(override val baseUrl: String) : Environment()
}
