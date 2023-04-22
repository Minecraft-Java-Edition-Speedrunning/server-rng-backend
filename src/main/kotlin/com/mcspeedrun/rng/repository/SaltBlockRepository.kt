package com.mcspeedrun.rng.repository

import com.mcspeedrun.rng.model.validation.SaltBlockEntry
import com.mcspeedrun.rng.model.http.http425
import database.generated.server_rng.Tables.SALT_BLOCK
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

val saltSource = SecureRandom()
private val encoder = Base64.getEncoder()
private val decoder = Base64.getDecoder()

private fun LocalDateTime.toEpochMilli(offset: ZoneOffset): Long {
    return this.toInstant(offset).toEpochMilli()
}

@Singleton
class SaltBlockRepository (
    @Property(name = "\${micronaut.application.saltLifetime}", defaultValue = "3600000")
    private val saltTTL: Long,
    private val jooq: DSLContext,
) {
    fun findExpiredSalt(time: LocalDateTime): SaltBlockEntry? {
        val now = LocalDateTime.now()
        if (time.isBefore(now)) {
            throw http425("can't read archived block from the future")
        }
        return jooq
            .select(
                SALT_BLOCK.ID,
                SALT_BLOCK.SALT,
                SALT_BLOCK.ACTIVE_AT,
                SALT_BLOCK.EXPIRES_AT,
                SALT_BLOCK.CREATED_AT,
            )
            .from(SALT_BLOCK)
            .where(SALT_BLOCK.EXPIRES_AT.lessThan(now))
            .and(DSL.value(time).between(SALT_BLOCK.EXPIRES_AT, SALT_BLOCK.ACTIVE_AT))
            .fetchOneInto(SaltBlockEntry::class.java)
    }

    fun findSalt(time: LocalDateTime): ByteArray {
        return jooq.transactionResult { transaction ->
            transaction.dsl()
                .select(SALT_BLOCK.SALT)
                .from(SALT_BLOCK)
                .where(DSL.value(time).between(SALT_BLOCK.EXPIRES_AT, SALT_BLOCK.ACTIVE_AT))
                .fetchOne()
                ?.component1()
                ?.let { decoder.decode(it) }?: createSalt(transaction, time)
        }
    }

    private fun createSalt(transaction: Configuration, time: LocalDateTime): ByteArray {
        val lastBlockEnd = transaction.dsl()
            .select(SALT_BLOCK.EXPIRES_AT)
            .from(SALT_BLOCK)
            .where(SALT_BLOCK.ACTIVE_AT.lessThan(time))
            .orderBy(SALT_BLOCK.ACTIVE_AT.desc())
            .fetchOne()
            ?.component1()
        val nextBlockStart = transaction.dsl()
            .select(SALT_BLOCK.ACTIVE_AT)
            .from(SALT_BLOCK)
            .where(SALT_BLOCK.EXPIRES_AT.greaterThan(time))
            .orderBy(SALT_BLOCK.ACTIVE_AT.asc())
            .fetchOne()
            ?.component1()

        val blockStart = lastBlockEnd?.let {
            val lastBlockEndEpoch = lastBlockEnd.toEpochMilli(ZoneOffset.UTC)
            val delta = time.toEpochMilli(ZoneOffset.UTC) - lastBlockEndEpoch
            val blocksBetween = delta.floorDiv(saltTTL)
            val instant = Instant.ofEpochMilli(lastBlockEndEpoch + (blocksBetween * saltTTL))
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        } ?: run {
            val epochTime = time.toEpochMilli(ZoneOffset.UTC)
            val instant = Instant.ofEpochMilli(epochTime - (epochTime % saltTTL))
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        }
        val blockEnd = blockStart
            .plus(saltTTL, ChronoUnit.MILLIS)
            .takeIf { nextBlockStart?.isAfter(it) ?: true } ?: nextBlockStart!!

        val salt = ByteArray(36).also { saltSource.nextBytes(it) }

        transaction.dsl()
            .insertInto(
                SALT_BLOCK,
                SALT_BLOCK.ID,
                SALT_BLOCK.SALT,
                SALT_BLOCK.ACTIVE_AT,
                SALT_BLOCK.EXPIRES_AT,
                SALT_BLOCK.CREATED_AT
            )
            .values(
                UUID.randomUUID().toString(),
                String(encoder.encode(salt)),
                blockStart,
                blockEnd,
                LocalDateTime.now(),
            )
            .execute()

        return salt
    }
}
