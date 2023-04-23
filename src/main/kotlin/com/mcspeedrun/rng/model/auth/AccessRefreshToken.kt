package com.mcspeedrun.rng.model.auth

import java.time.Instant

data class AccessRefreshToken (
    val refreshToken: String,
    val accessToken: String,
    val expiresAt: Instant,
)
