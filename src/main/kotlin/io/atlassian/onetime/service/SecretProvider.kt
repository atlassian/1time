package io.atlassian.onetime.service

import io.atlassian.onetime.model.TOTPSecret
import java.security.SecureRandom

fun interface SecretProvider {
    suspend fun generateSecret(): TOTPSecret
}

class AsciiRangeSecretProvider : SecretProvider {

    companion object {
        val ASCII_RANGE: CharRange = (' '..'z')
    }

    override suspend fun generateSecret() = TOTPSecret(
        (1..20).map { ASCII_RANGE.random() }.joinToString("").toByteArray()
    )
}

class RandomSecretProvider : SecretProvider {

    override suspend fun generateSecret() =
        SecureRandom().let {
            val byteArray = ByteArray(20)
            it.nextBytes(byteArray)
            TOTPSecret(byteArray)
        }
}
