package com.mcspeedrun.rng.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jwt.*
import io.micronaut.security.token.generator.TokenGenerator
import io.micronaut.security.token.jwt.encryption.EncryptionConfiguration
import io.micronaut.security.token.jwt.signature.SignatureConfiguration
import io.micronaut.security.token.jwt.signature.jwks.JwksCache
import jakarta.inject.Singleton
import java.text.ParseException
import java.util.*

/**
 * JWT validator based off of io.micronaut.security.token.jwt.validator.JwtValidator
 */
@Singleton
class JWTService (
    private val tokenGenerator: TokenGenerator,
    private val signatureConfigs: List<SignatureConfiguration>,
    private val encryptionConfigs: List<EncryptionConfiguration>,
    private val objectMapper: ObjectMapper,
) {
    fun generate(target: Any): String? {
        @Suppress("UNCHECKED_CAST")
        val objectMap = objectMapper.convertValue(target, Map::class.java) as Map<String, Any>?
        return tokenGenerator.generateToken(objectMap).takeIf { it.isPresent }?.get()
    }

    inline fun <reified T> validate(token: String): T? {
        return validate(token, T::class.java)
    }

    fun <T> validate(token: String, toValueType: Class<T>): T? {
        return token
            .takeIf { hasAtLeastTwoDots(it) }
            ?.let { parse(it) }
            ?.let { validate(it) }
            ?.jwtClaimsSet?.claims
            ?.let { objectMapper.convertValue(it, toValueType) }
    }

    private fun hasAtLeastTwoDots(token: String): Boolean {
        val firstDot = token.indexOf(".")
        if (firstDot == -1) {
            return false
        }
        return token.indexOf(".", firstDot + 1) != -1
    }

    private fun parse(token: String): JWT? {
        return try {
            JWTParser.parse(token)
        }
        catch (e: ParseException) {
            null
        }
    }

    private fun validate(token: JWT): JWT? {
        return when(token) {
            // plain jwt's can only be validated if no signatures have been defined
            is PlainJWT -> token.takeIf { signatureConfigs.isEmpty() }
            is EncryptedJWT -> validate(token)
            is SignedJWT -> validate(token)
            else -> null
        }
    }

    private fun validate(jwt: EncryptedJWT): JWT? {
        val header = jwt.header
        val configs = encryptionConfigs.sortedWith(comparator(header))
        for (config in configs) {
            try {
                config.decrypt(jwt)
                jwt.payload.toSignedJWT()
                    ?.let { validate(it) }
                    ?.let { return it }
            } catch (ignored: JOSEException) {}
        }
        return null
    }

    private fun validate(jwt: SignedJWT): SignedJWT? {
        val algorithm = jwt.header.algorithm
        val configs = signatureConfigs.sortedWith(comparator(algorithm, jwt.header.keyID))
        for (config in configs) {
            if (config is JwksCache && config.isExpired) {
                config.clear()
            }
            try {
                if (config.verify(jwt)) {
                    return jwt
                }
            } catch (_: JOSEException) { }
        }
        return null
    }

    companion object {
        private fun compareKeyIds(
            sig: SignatureConfiguration,
            otherSig: SignatureConfiguration,
            keyId: String?
        ): Int {
            if (keyId == null) {
                return 0
            }
            val matchesKeyId = signatureConfigurationMatchesKeyId(sig, keyId)
            val otherMatchesKeyId = signatureConfigurationMatchesKeyId(otherSig, keyId)

            return matchesKeyId.compareTo(otherMatchesKeyId)
        }

        private fun comparator(algorithm: JWSAlgorithm, kid: String?): Comparator<SignatureConfiguration> {
            return Comparator { sig: SignatureConfiguration, otherSig: SignatureConfiguration ->
                val compareKids = compareKeyIds(sig, otherSig, kid)
                if (compareKids != 0) {
                    return@Comparator compareKids
                }
                val supports = signatureConfigurationSupportsAlgorithm(sig, algorithm)
                val otherSupports = signatureConfigurationSupportsAlgorithm(otherSig, algorithm)
                return@Comparator supports.compareTo(otherSupports)
            }
        }

        private fun signatureConfigurationMatchesKeyId(
            sig: SignatureConfiguration,
            keyId: String
        ): Boolean {
            return Optional.of(sig)
                .filter { sig is JwksCache }
                .flatMap { (sig as JwksCache).keyIds }
                .map { it.contains(keyId) }
                .orElse(false)
        }

        private fun signatureConfigurationSupportsAlgorithm(
            sig: SignatureConfiguration,
            algorithm: JWSAlgorithm
        ): Boolean {
            // {@link JwksSignature#supports} does an HTTP request if the Json Web Key Set is not present.
            // Thus, don't call it unless the keys have been already been fetched.
            return if (sig !is JwksCache || (sig as JwksCache).isPresent) {
                sig.supports(algorithm)
            } else false
        }

        private fun comparator(header: JWEHeader): Comparator<EncryptionConfiguration> {
            val algorithm = header.algorithm
            val method = header.encryptionMethod
            return Comparator { sig: EncryptionConfiguration, otherSig: EncryptionConfiguration ->
                val supports = sig.supports(algorithm, method)
                val otherSupports = otherSig.supports(algorithm, method)
                return@Comparator supports.compareTo(otherSupports)
            }
        }
    }
}
