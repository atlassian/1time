package com.atlassian.onetime.service

import com.atlassian.onetime.model.TOTPSecret
import java.util.concurrent.CompletableFuture

fun interface SecretProvider {
  fun generateSecret(): TOTPSecret
}

fun interface AsyncSecretProvider {
  fun generateSecret(): CompletableFuture<TOTPSecret>
}

fun interface CPSSecretProvider {
  suspend fun generateSecret(): TOTPSecret
}
