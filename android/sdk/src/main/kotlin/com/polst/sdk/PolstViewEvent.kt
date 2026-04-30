package com.polst.sdk

public sealed class PolstViewEvent {
    public data class Vote(val vote: com.polst.sdk.Vote) : PolstViewEvent()
    public data class StepVote(
        val campaignId: String,
        val stepIndex: Int,
        val vote: com.polst.sdk.Vote,
    ) : PolstViewEvent()
    public data class Complete(
        val campaignId: String,
        val votes: List<com.polst.sdk.Vote>,
    ) : PolstViewEvent()
}
