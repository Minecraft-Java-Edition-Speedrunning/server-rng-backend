package com.mcspeedrun.rng.model.validation

import java.time.LocalDateTime

data class SaltBlockEntry (
    val id: String,
    val salt: String,
    val activeAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
