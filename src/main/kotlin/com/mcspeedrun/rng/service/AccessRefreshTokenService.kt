package com.mcspeedrun.rng.service

import com.mcspeedrun.rng.model.auth.AccessRefreshToken
import com.nimbusds.jwt.JWTClaimsSet
import io.micronaut.context.env.Environment
import io.micronaut.runtime.ApplicationConfiguration
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.config.TokenConfiguration
import io.micronaut.security.token.generator.TokenGenerator
import io.micronaut.security.token.jwt.generator.AccessTokenConfiguration
import io.micronaut.security.token.jwt.generator.claims.ClaimsAudienceProvider
import io.micronaut.security.token.jwt.generator.claims.JwtIdGenerator
import jakarta.inject.Singleton
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


@Singleton
class AccessRefreshTokenService (
    private val tokenGenerator: TokenGenerator,
    private val accessTokenConfiguration: AccessTokenConfiguration,
    private var jwtIdGenerator: JwtIdGenerator?,
    private val claimsAudienceProvider: ClaimsAudienceProvider?,
    private val tokenConfiguration: TokenConfiguration,
    applicationConfiguration : ApplicationConfiguration?,
) {
    private val applicationName = (applicationConfiguration?.name?.orElse(null) ?: Environment.MICRONAUT)

    fun generate(
        refreshToken: String,
        authentication: Authentication,
    ): AccessRefreshToken? {
        val expiresIn = accessTokenConfiguration.expiration
        val now = Instant.now()
        val expiresAt = now.plus(expiresIn.toLong(), ChronoUnit.MILLIS)
        val builder = JWTClaimsSet.Builder()
        builder.issueTime(Date.from(now))
        builder.notBeforeTime(Date.from(now))
        builder.expirationTime(Date.from(expiresAt))
        jwtIdGenerator?.let {
            builder.jwtID(it.generateJtiClaim())
        }
        claimsAudienceProvider?.let {
            builder.audience(it.audience())
        }
        builder.issuer(applicationName)

        builder.subject(authentication.name)
        authentication.attributes.forEach { (name: String?, value: Any?) ->
            builder.claim(
                name,
                value
            )
        }
        val rolesKey: String = tokenConfiguration.rolesName
        if (!rolesKey.equals(TokenConfiguration.DEFAULT_ROLES_NAME, ignoreCase = true)) {
            builder.claim("rolesKey", rolesKey)
        }
        builder.claim(rolesKey, authentication.roles)
        val accessToken = tokenGenerator.generateToken(builder.build().claims)
            .takeIf { it.isPresent }?.get() ?: return null
        return AccessRefreshToken(refreshToken, accessToken, expiresAt.toEpochMilli())
    }
}
