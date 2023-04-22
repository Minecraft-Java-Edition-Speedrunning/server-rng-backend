package com.mcspeedrun.rng.model

import java.time.LocalDateTime

data class SaltEntry (
    val id: String,
    val salt: String,
    val activeAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
