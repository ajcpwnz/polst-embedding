package com.polst.sdk.network.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object InstantSerializer : KSerializer<java.time.Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: java.time.Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.time.Instant =
        java.time.Instant.parse(decoder.decodeString())
}
