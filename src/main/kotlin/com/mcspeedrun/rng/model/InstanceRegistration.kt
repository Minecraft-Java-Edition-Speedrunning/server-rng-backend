package com.mcspeedrun.rng.model

import java.time.LocalDateTime

data class InstanceRegistration (
    val id: String,
    val userId: String,
    val authentication: Int,
    val refreshTokenKey: String,
    val invalidated: Boolean,
    val refreshedAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
