package com.mcspeedrun.rng.model

import java.time.LocalDateTime
import kotlin.random.Random

data class RandomSource(
    val sourceId: String,
    private val seed: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    var uses: Int = 0
    var source: Random = TODO("replace with random from seed")
}