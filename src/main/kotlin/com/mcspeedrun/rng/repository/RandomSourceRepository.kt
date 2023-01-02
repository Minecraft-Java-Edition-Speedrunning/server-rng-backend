package com.mcspeedrun.rng.repository

import com.mcspeedrun.rng.model.RandomSource
import database.generated.server_rng.Tables.RANDOM_SOURCE
import jakarta.inject.Singleton
import org.jooq.DSLContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

@Singleton
class RandomSourceRepository (
    private val jooq: DSLContext,
) {
    fun createSource(seed: String, duration: Duration): RandomSource {
        val createdAt = LocalDateTime.now(ZoneOffset.UTC)
        val expiresAt = createdAt.plus(duration)
        return jooq
            .insertInto(RANDOM_SOURCE, RANDOM_SOURCE.SEED, RANDOM_SOURCE.CREATED_AT, RANDOM_SOURCE.EXPIRES_AT)
            .values(
                seed,
                createdAt,
                expiresAt,
            )
            .returning()
            .fetchInto(RandomSource::class.java)
            .first()
    }
}
