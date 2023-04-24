package com.mcspeedrun.rng.service

import com.mcspeedrun.rng.model.YggdrasilRegistration
import com.mcspeedrun.rng.model.http.http403
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*

class YggdrasilService {
    private val decoder = Base64.getDecoder()

    private val yggdrasilKeyString = """
        MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAylB4B6m5lz7jwrcFz6Fd/fnfUhcvlxsTSn5kIK/2aGG1C3kMy4VjhwlxF6BFUSnfxhNs
        wPjh3ZitkBxEAFY25uzkJFRwHwVA9mdwjashXILtR6OqdLXXFVyUPIURLOSWqGNBtb08EN5fMnG8iFLgEJIBMxs9BvF3s3/FhuHyPKiVTZmXY0WY
        4ZyYqvoKR+XjaTRPPvBsDa4WI2u1zxXMeHlodT3lnCzVvyOYBLXL6CJgByuOxccJ8hnXfF9yY4F0aeL080Jz/3+EBNG8RO4ByhtBf4Ny8NQ6stWs
        jfeUIvH7bU/4zCYcYOq4WrInXHqS8qruDmIl7P5XXGcabuzQstPf/h2CRAUpP/PlHXcMlvewjmGU6MfDK+lifScNYwjPxRo4nKTGFZf/0aqHCh/E
        AsQyLKrOIYRE0lDG3bzBh8ogIMLAugsAfBb6M3mqCqKaTMAf/VAjh5FFJnjS+7bE+bZEV0qwax1CEoPPJL1fIQjOS8zj086gjpGRCtSy9+bTPTfT
        R/SJ+VUB5G2IeCItkNHpJX2ygojFZ9n5Fnj7R9ZnOM+L8nyIjPu3aePvtcrXlyLhH/hvOfIOjPxOlqW+O5QwSFP4OEcyLAUgDdUgyW36Z5mB285u
        KW/ighzZsOTevVUG2QwDItObIV6i8RCxFbN2oDHyPaO5j1tTaBNyVt8CAwEAAQ==""".filter { !it.isWhitespace() }
    private val yggdrasilKeyBytes = decoder.decode(yggdrasilKeyString)
    private val yggdrasilKeySpec = X509EncodedKeySpec(yggdrasilKeyBytes)

    fun validate(
        request: YggdrasilRegistration,
    ): Boolean {
        val publicKeyBytes = decoder.decode(request.publicKey)
        val challengeBytes = decoder.decode(request.challenge)
        val keySignatureBytes = decoder.decode(request.keySignature)
        val challengeSignatureBytes = decoder.decode(request.challengeSignature)

        val uuid = UUID.fromString(request.uuid)
        val uuidBuffer = ByteBuffer.wrap(ByteArray(Long.SIZE_BYTES * 2))
        uuidBuffer.putLong(uuid.mostSignificantBits)
        uuidBuffer.putLong(uuid.leastSignificantBits)
        val uuidBytes = uuidBuffer.array()

        val isKeyOwner = validateKeyOwner(
            uuidBytes,
            publicKeyBytes,
            request.keyExpiration,
            keySignatureBytes,
        )
        if (!isKeyOwner) {
            throw http403("public key is not valid for uuid")
        }

        return validateChallenge(
            request.uuid,
            publicKeyBytes,
            challengeBytes,
            request.challengeExpiration,
            challengeSignatureBytes,
        )
    }

    private fun validateChallenge(
        uuid: String,
        publicKeyBytes: ByteArray,
        challengeBytes: ByteArray,
        expiration: Long,
        signatureBytes: ByteArray,
    ): Boolean {
        val expired = Instant.now().isAfter(Instant.ofEpochMilli(expiration))
        if (expired) {
            return false
        }

        // TODO: make sure challenge has not been used before for this user

        val expirationBytes = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(expiration).array()

        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val signature = Signature.getInstance("RSA-SHA1")

        signature.initVerify(publicKey)
        signature.update(challengeBytes)
        signature.update(expirationBytes)
        return signature.verify(signatureBytes)
    }

    private fun validateKeyOwner(
        ownerBytes: ByteArray,
        publicKeyBytes: ByteArray,
        expiration: Long,
        signatureBytes: ByteArray,
    ): Boolean {
        val expired = Instant.now().isAfter(Instant.ofEpochMilli(expiration))
        if (expired) {
            return false
        }

        val expirationBytes = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(expiration).array()

        val keyFactory = KeyFactory.getInstance("RSA")
        val yggdrasilPublicKey = keyFactory.generatePublic(yggdrasilKeySpec)

        val signature = Signature.getInstance("RSA-SHA1")

        signature.initVerify(yggdrasilPublicKey)
        signature.update(ownerBytes) // uuid
        signature.update(expirationBytes)
        signature.update(publicKeyBytes)
        return signature.verify(signatureBytes)
    }
}
