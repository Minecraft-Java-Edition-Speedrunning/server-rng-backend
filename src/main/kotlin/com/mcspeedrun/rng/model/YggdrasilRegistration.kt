package com.mcspeedrun.rng.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class YggdrasilRegistration (
    val uuid: String,
    val publicKey: String,
    val keyExpiration: Long,
    val keySignature: String,
    val challenge: String,
    val challengeExpiration: Long,
    val challengeSignature: String,
)
