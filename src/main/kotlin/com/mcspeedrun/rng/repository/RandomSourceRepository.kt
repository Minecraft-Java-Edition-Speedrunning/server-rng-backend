package com.mcspeedrun.rng.repository

import com.mcspeedrun.rng.model.RandomSource
import com.mcspeedrun.rng.model.validation.RandomSourceEntry
import com.mcspeedrun.rng.model.http.http425
import database.generated.server_rng.Tables.RANDOM_SOURCE
import jakarta.inject.Singleton
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

@Singleton
class RandomSourceRepository (
    private val jooq: DSLContext,
) {
    fun findExpiredSource(time: LocalDateTime): List<RandomSourceEntry> {
        val now = LocalDateTime.now()
        if (time.isBefore(now)) {
            throw http425("can't read archived random source from the future")
        }
        return jooq
            .select(
                RANDOM_SOURCE.ID,
                RANDOM_SOURCE.SEED,
                RANDOM_SOURCE.EXPIRES_AT,
                RANDOM_SOURCE.CREATED_AT,
            )
            .from(RANDOM_SOURCE)
            .where(RANDOM_SOURCE.EXPIRES_AT.lessThan(now))
            .and(DSL.value(time).between(RANDOM_SOURCE.EXPIRES_AT, RANDOM_SOURCE.CREATED_AT))
            .fetchInto(RandomSourceEntry::class.java)
    }

    fun createSource(seed: String, duration: Duration): RandomSource {
        // TODO("store server instance source was generated on")
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
