package com.mcspeedrun.rng.service

import com.mcspeedrun.rng.model.Access
import com.mcspeedrun.rng.model.AuthenticationMethod
import com.mcspeedrun.rng.model.auth.AccessRefreshToken
import com.mcspeedrun.rng.model.http.http401
import com.mcspeedrun.rng.model.http.http429
import com.mcspeedrun.rng.model.http.http500
import com.mcspeedrun.rng.repository.RegisteredInstancesRepository
import io.micronaut.context.annotation.Property
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.generator.RefreshTokenGenerator
import io.micronaut.security.token.validator.RefreshTokenValidator
import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


@Singleton
class AuthenticationService(
    @Property(name = "\${micronaut.application.credentialLimit}", defaultValue = "20")
    private val credentialLimit: Int,
    private val repository: RegisteredInstancesRepository,
    private var tokenGenerator: AccessRefreshTokenService,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val refreshTokenValidator: RefreshTokenValidator,
) {
    private fun getUserIdentifier(method: AuthenticationMethod, userId: String): Long {
        return repository.getUserIdentifier(method, userId)?.also { identifier ->
            val registeredInstances = repository.getRegisteredInstances(identifier)
            val rateCutoff = LocalDateTime.now(ZoneOffset.UTC).minusHours(1)
            val recentlyRegistered = registeredInstances.filter { it.createdAt.isAfter(rateCutoff) }
            if (recentlyRegistered.size > credentialLimit) {
                throw http429("exceeded maximum login requests of $credentialLimit/hr")
            }
            if (registeredInstances.size > credentialLimit) {
                repository.trimRegisteredInstances(identifier)
            }
        } ?: repository.createUserIdentifier(method, userId)
    }

    fun registerInstance(method: AuthenticationMethod, userId: String): AccessRefreshToken {
        val identifier = getUserIdentifier(method, userId)

        val instanceId = UUID.randomUUID().toString()
        val access = listOf(Access.RUN)

        val authentication = Authentication.build(instanceId, mapOf(Pair("acc", Access.serialize(access))))
        val refreshTokenKey: String = refreshTokenGenerator.createKey(authentication)
        val refreshToken = refreshTokenGenerator.generate(authentication, refreshTokenKey)
            .takeIf { it.isPresent }?.get() ?: throw http500("unable to generate refresh token")
        val accessRefreshToken = tokenGenerator.generate(refreshToken, authentication)
            ?: throw http500("unable to generate access token")

        repository.saveRegisteredInstance(instanceId, identifier, refreshTokenKey, access)
        return accessRefreshToken
    }

    fun refreshInstance(refreshToken: String): AccessRefreshToken {
        val refreshTokenKey = refreshTokenValidator.validate(refreshToken)
            .takeIf { it.isPresent }?.get() ?: throw http401("unable to validate refresh token")

        val instanceId = repository.getRefreshTokenInstance(refreshTokenKey) ?: throw http401("unable to find session")
        val roles = repository.getAccess(instanceId)
        val authentication = Authentication.build(instanceId, roles.map { it.name })

        val newRefreshTokenKey: String = refreshTokenGenerator.createKey(authentication)
        val newRefreshToken =refreshTokenGenerator.generate(authentication, newRefreshTokenKey)
            .takeIf { it.isPresent }?.get() ?: throw http500("unable to generate refresh token")

        repository.refreshInstance(instanceId, newRefreshTokenKey)
        return tokenGenerator.generate(newRefreshToken, authentication)
            ?: throw http500("unable to generate access token")
    }
}
