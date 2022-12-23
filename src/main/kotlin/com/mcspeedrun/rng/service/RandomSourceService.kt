package com.mcspeedrun.rng.service

import com.mcspeedrun.rng.model.RandomSource
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton

/**
 * source table should contain the following rows
 * sourceId: UUID - primary key
 * source: String - random data that source is generated from
 * createdAt: Instant - time the source was created at
 * closed: Instant - time the source was closed at
 */
@Singleton
class RandomSourceService (
    // how many times a source can be used before it is deemed unusable
    @Property(name = "\${micronaut.application.sourceUsageLimit}", defaultValue = "100")
    val sourceUsageLimit: Int,
    // how long after a source was created till it is deemed unusable
    @Property(name = "\${micronaut.application.sourceLifetime}", defaultValue = "3600000")
    val sourceTTL: Long,
) {
    fun getRandom(): RandomSource {
        TODO("get random based on source if source has uses left otherwise get new source")
    }
}