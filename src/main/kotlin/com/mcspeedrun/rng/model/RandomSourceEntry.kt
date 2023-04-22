package com.mcspeedrun.rng.model

import java.time.LocalDateTime

data class RandomSourceEntry (
    val id: String,
    val seed: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
