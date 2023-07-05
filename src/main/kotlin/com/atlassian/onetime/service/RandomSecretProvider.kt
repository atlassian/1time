package com.atlassian.onetime.service

import com.atlassian.onetime.model.TOTPSecret
import java.security.SecureRandom
import java.util.concurrent.CompletableFuture

class RandomSecretProvider : SecretProvider {

  companion object {
    fun generateSecret() =
      SecureRandom().let {
        val byteArray = ByteArray(20)
        it.nextBytes(byteArray)
        TOTPSecret(byteArray)
      }
  }

  override fun generateSecret() = RandomSecretProvider.generateSecret()
}

class AsyncRandomSecretProvider : AsyncSecretProvider {

  override fun generateSecret(): CompletableFuture<TOTPSecret> =
    CompletableFuture.supplyAsync {
      RandomSecretProvider.generateSecret()
    }
}

class CPSRandomSecretProvider : CPSSecretProvider {

  override suspend fun generateSecret(): TOTPSecret = RandomSecretProvider.generateSecret()
}
