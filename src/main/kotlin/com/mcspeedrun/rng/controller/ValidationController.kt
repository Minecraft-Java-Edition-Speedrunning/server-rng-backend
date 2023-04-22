package com.mcspeedrun.rng.controller

import com.mcspeedrun.rng.model.validation.RandomSourceEntry
import com.mcspeedrun.rng.repository.SaltBlockRepository
import com.mcspeedrun.rng.model.validation.SaltBlockEntry
import com.mcspeedrun.rng.repository.RandomSourceRepository
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import java.time.LocalDateTime

@Suppress("Unused")
@Controller("/api/v2/validation")
class ValidationController (
    private val saltBlockRepository: SaltBlockRepository,
    private val randomSourceRepository: RandomSourceRepository,
) {
    @Get("salt")
    fun getSaltBlock(
        @QueryValue("blockTime") blockTime: LocalDateTime
    ): SaltBlockEntry? {
        return saltBlockRepository.findExpiredSalt(blockTime)
    }

    @Get("random_source")
    fun getRandomSource(
        @QueryValue("sourceTime") sourceTime: LocalDateTime
    ): List<RandomSourceEntry> {
        // TODO("ability to 1filter by server instance")
        return randomSourceRepository.findExpiredSource(sourceTime)
    }
}
