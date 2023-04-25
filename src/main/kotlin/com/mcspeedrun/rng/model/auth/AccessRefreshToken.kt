package com.mcspeedrun.rng.model.auth

data class AccessRefreshToken (
    val refreshToken: String,
    val accessToken: String,
    val expiresAt: Long,
)
