package com.mcspeedrun.rng.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class YggdrasilRegistration (
    val uuid: String,
    val challenge: String,
    val response: String,
    val publicKey: String,
    val signature: String,
    val instant: String,
)
