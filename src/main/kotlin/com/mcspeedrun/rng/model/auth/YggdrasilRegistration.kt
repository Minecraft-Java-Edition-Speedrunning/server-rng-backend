package com.mcspeedrun.rng.model.auth

import io.micronaut.core.annotation.Introspected

@Introspected
data class YggdrasilRegistration (
    val uuid: String,
    val publicKey: String,
    val publicKeyExpiration: Long,
    val publicKeySignature: String,
    val challenge: String,
    val challengeExpiration: Long,
    val challengeSignature: String,
)
