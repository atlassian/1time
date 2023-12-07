package com.atlassian.onetime.service

import com.atlassian.onetime.model.TOTPSecret
import java.util.concurrent.CompletableFuture

class AsciiRangeSecretProvider : SecretProvider {
  companion object {
    private val ASCII_RANGE: CharRange = (' '..'z')

    fun generateSecret() =
      TOTPSecret(
        (1..20).map { ASCII_RANGE.random() }.joinToString("").toByteArray(),
      )
  }

  override fun generateSecret() = AsciiRangeSecretProvider.generateSecret()
}

class AsyncAsciiRangeSecretProvider : AsyncSecretProvider {
  override fun generateSecret(): CompletableFuture<TOTPSecret> =
    CompletableFuture.supplyAsync {
      AsciiRangeSecretProvider.generateSecret()
    }
}

class CPSAsciiRangeSecretProvider : CPSSecretProvider {
  override suspend fun generateSecret(): TOTPSecret = AsciiRangeSecretProvider.generateSecret()
}
