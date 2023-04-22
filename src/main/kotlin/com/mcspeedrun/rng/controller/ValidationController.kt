package com.mcspeedrun.rng.controller

import com.mcspeedrun.rng.repository.SaltBlockRepository
import com.mcspeedrun.rng.model.SaltBlockEntry
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import java.time.LocalDateTime

@Suppress("Unused")
@Controller("/api/v2/validation")
class ValidationController (
    private val saltBlockRepository: SaltBlockRepository,
) {
    @Get("salt")
    fun getSaltBlock(
        @QueryValue("blockTime") blockTime: LocalDateTime
    ): SaltBlockEntry? {
        return saltBlockRepository.findExpiredSalt(blockTime)
    }
    // TODO("endpoint for getting random source blocks after they have expired")
}
