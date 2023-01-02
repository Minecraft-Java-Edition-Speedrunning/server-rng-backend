package com.mcspeedrun.rng.model

import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

private val decoder = Base64.getDecoder()

data class RandomSource (
    val sourceId: String,
    private val seed: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    var uses: Int = 0
    var source: SecureRandom = SecureRandom.getInstance("SHA1PRNG").also{ it.setSeed(decoder.decode(seed)) }
}
