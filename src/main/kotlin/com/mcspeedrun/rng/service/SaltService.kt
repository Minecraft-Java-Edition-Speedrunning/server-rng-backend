package com.mcspeedrun.rng.service

import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.security.MessageDigest
import java.time.Instant
import java.util.*

/**
 * salt table should contain the following rows
 * saltId: UUID - primary key
 * salt: String - random data that for the salt
 * openedAt: Instant - time the salt was opened
 * closedAt: Instant - time the salt was closed
 */

@Singleton
class SaltService (
    @Property(name = "\${micronaut.application.saltHashAlg}", defaultValue = "SHA-256")
    val saltHashAlg: String,
    @Property(name = "\${micronaut.application.saltLifetime}", defaultValue = "3600000")
    val saltTTL: Long,
) {
    // TODO("salt cache from db")
    // TODO("generate salt on chron job")
    private fun getSalt(saltTime: Instant): Long {
        TODO("get the active salt at the time from cache or db$saltTime")
    }

    fun getHashAlg(): String {
        return saltHashAlg
    }

    fun getBlockSeed(blockStartTime: Instant, runSalt: String, block: Int): String {
        val globalSalt = getSalt(blockStartTime)
        val digest = MessageDigest.getInstance(saltHashAlg)
        digest.update(globalSalt.toByte())
        digest.update(runSalt.toByte())
        digest.update(block.toByte())

        return String(Base64.getEncoder().encode(digest.digest()))
    }
}