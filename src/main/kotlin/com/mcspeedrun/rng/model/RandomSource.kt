package com.mcspeedrun.rng.model

import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

private val decoder = Base64.getDecoder()

data class RandomSource (
    val sourceId: Long,
    private val seed: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
) {
    var uses: Int = 0
    var source: SecureRandom = SecureRandom.getInstance("SHA1PRNG").also{ it.setSeed(decoder.decode(seed)) }
}
