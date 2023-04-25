package com.mcspeedrun.rng.controller

import com.mcspeedrun.rng.model.http.http403
import com.mcspeedrun.rng.model.http.http500
import com.mcspeedrun.rng.repository.RegisteredInstancesRepository
import com.mcspeedrun.rng.service.JWTService
import com.mcspeedrun.rng.service.RandomSourceService
import com.mcspeedrun.rng.service.SaltService
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.security.Principal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

const val DEFAULT_BLOCK_SIZE: Long = 30000

@Introspected
data class TokenResponse<T> (
    val data: T,
    val token: String,
)

@Introspected
data class RunStart (
    val instance: String,
    val isSetSeed: Boolean,
    val seed: String,
    val randomSalt: String,
    val randomSourceId: Long,
    val runId: String = UUID.randomUUID().toString(),
    val startTime: Instant = Instant.now(),
    val blockSize: Long,
)

@Introspected
data class RunForm<T> (
    val runToken: String,
    val data: T,
)
@Introspected
data class RandomBlockForm (
    val block: Long,
)

@Introspected
data class RandomBlock (
    val runId: String,
    val block: Long,
    val seed: String,
    val hashAlgorithm: String,
)

@Introspected
data class TimeboxForm (
    val hash: String,
    val cause: String,
)

@Introspected
data class Timebox (
    val runId: String,
    val hash: String,
    val cause: String,
    val time: Instant = Instant.now(),
)

@Suppress("Unused")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api/v2/verification")
class VerificationController (
    private val jwtService: JWTService,
    private val randomSourceService: RandomSourceService,
    private val saltService: SaltService,
    private val registeredInstancesRepository: RegisteredInstancesRepository,
) {
    @Post("start_run")
    fun startRun(
        @QueryValue("seed") setSeed: String?,
        instance: Principal,
    ): TokenResponse<RunStart> {
        registeredInstancesRepository.startRun(instance.name)

        val randomSource = randomSourceService.getRandom()
        val runStart = RunStart(
            instance = instance.name,
            isSetSeed = setSeed != null,
            seed = setSeed ?: randomSource.worldSeed,
            randomSalt = randomSource.randomSalt,
            randomSourceId = randomSource.sourceId,
            blockSize = DEFAULT_BLOCK_SIZE,
        )

        return tokenResponse(runStart)
    }
    @Post("get_random")
    fun getRandom(
        instance: Principal,
        @Body blockForm: RunForm<RandomBlockForm>,
    ): TokenResponse<RandomBlock> {
        val run: RunStart = jwtService.validate(blockForm.runToken) ?: throw http500("unable to parse token")
        if (instance.name != run.instance) {
            throw http403("tokens instance does not match current instance")
        }
        val maxBlock = (Duration.between(run.startTime, Instant.now()).toMillis() / run.blockSize) + 1
        if (blockForm.data.block > maxBlock) {
            throw http403("target block can not be retrieve yet")
        }
        val blockStartTime = ChronoUnit.MILLIS.addTo(run.startTime, run.blockSize)
        val randomBlock = RandomBlock(
            runId = run.runId,
            block = blockForm.data.block,
            seed = saltService.getBlockSeed(blockStartTime, runSalt = run.randomSalt, blockForm.data.block),
            hashAlgorithm = saltService.getHashAlg(),
        )
        return tokenResponse(randomBlock)
    }
    @Post("timebox_run")
    fun timeboxRun(
        instance: Principal,
        @Body timeboxForm: RunForm<TimeboxForm>,
    ): TokenResponse<Timebox> {
        val run: RunStart = jwtService.validate(timeboxForm.runToken) ?: throw http500("unable to parse token")
        if (instance.name != run.instance) {
            throw http403("tokens instance does not match current instance")
        }
        val timebox = Timebox(
            runId = run.runId,
            hash = timeboxForm.data.hash,
            cause = timeboxForm.data.cause,
        )
        return tokenResponse(timebox)
    }

    private fun <T: Any> tokenResponse(data: T): TokenResponse<T> {
        val token = jwtService.generate(data)
            ?: throw http500("unable to sign token")
        return TokenResponse(data, token)
    }
}
