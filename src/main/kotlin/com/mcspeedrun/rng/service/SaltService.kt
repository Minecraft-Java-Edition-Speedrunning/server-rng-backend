package com.mcspeedrun.rng.service

import com.mcspeedrun.rng.repository.SaltBlockRepository
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Singleton
class SaltService (
    @Property(name = "\${micronaut.application.saltHashAlg}", defaultValue = "SHA-256")
    private val saltHashAlg: String,
    private val repository: SaltBlockRepository,
) {
    private fun getSalt(saltTime: Instant): ByteArray {
        return repository.findSalt(LocalDateTime.ofInstant(saltTime, ZoneOffset.UTC))
    }

    fun getHashAlg(): String {
        return saltHashAlg
    }

    fun getBlockSeed(blockStartTime: Instant, runSalt: String, block: Long): String {
        val globalSalt = getSalt(blockStartTime)
        val digest = MessageDigest.getInstance(saltHashAlg)
        digest.update(globalSalt)
        digest.update(runSalt.toByte())
        digest.update(block.toByte())

        return String(Base64.getEncoder().encode(digest.digest()))
    }
}
