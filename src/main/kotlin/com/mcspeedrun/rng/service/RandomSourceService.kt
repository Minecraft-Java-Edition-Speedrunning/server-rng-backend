package com.mcspeedrun.rng.service

import com.mcspeedrun.rng.model.RandomSource
import com.mcspeedrun.rng.model.RunRandomSource
import com.mcspeedrun.rng.repository.RandomSourceRepository
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

val seedSource = SecureRandom()
private val encoder = Base64.getEncoder()

@Singleton
class RandomSourceService (
    // how many times a source can be used before it is deemed unusable
    @Property(name = "\${micronaut.application.sourceUsageLimit}", defaultValue = "100")
    private val sourceUsageLimit: Int,
    // how long after a source was created till it is deemed unusable
    @Property(name = "\${micronaut.application.sourceLifetime}", defaultValue = "3600000")
    private val sourceTTL: Long,
    private val repository: RandomSourceRepository,
) {
    private val duration = Duration.of(sourceTTL, ChronoUnit.MILLIS)
    private var source: RandomSource? = null

    private fun generateSeed(): String {
        return String(encoder.encode(seedSource.generateSeed(36)))
    }

    private fun getSource(): RandomSource {
        return source
            ?.takeIf { it.uses < sourceUsageLimit }
            ?.takeIf { it.expiresAt.isBefore(LocalDateTime.now()) }
            ?: repository.createSource(generateSeed(), duration)
                .also {
                    source = it
                }
    }

    fun getRandom(): RunRandomSource {
        val source = getSource()

        return RunRandomSource(
            source.sourceId,
            String(encoder.encode(source.source.nextBytes(36))),
            String(encoder.encode(source.source.nextBytes(36))),
        )
    }
}