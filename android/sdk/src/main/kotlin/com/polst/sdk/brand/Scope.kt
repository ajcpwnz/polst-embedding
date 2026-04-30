package com.polst.sdk.brand

public enum class Scope(public val wireValue: String) {
    PolstsRead("polsts:read"),
    PolstsWrite("polsts:write"),
    CampaignsRead("campaigns:read"),
    CampaignsWrite("campaigns:write"),
    WebhooksRead("webhooks:read"),
    WebhooksWrite("webhooks:write"),
    AnalyticsRead("analytics:read"),
    ;

    public companion object {
        public fun fromWireValue(wire: String): Scope? = entries.firstOrNull { it.wireValue == wire }
    }
}
