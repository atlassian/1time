package com.atlassian.onetime.com.atlassian.onetime.service

import com.atlassian.onetime.model.TOTPSecret
import com.atlassian.onetime.service.AsciiRangeSecretProvider
import com.atlassian.onetime.service.AsyncAsciiRangeSecretProvider
import com.atlassian.onetime.service.AsyncRandomSecretProvider
import com.atlassian.onetime.service.CPSAsciiRangeSecretProvider
import com.atlassian.onetime.service.CPSRandomSecretProvider
import com.atlassian.onetime.service.RandomSecretProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.char.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking

class SecretProviderTest : FunSpec({

  context("Secret providers") {
    test("should generate secret correctly") {
      checkAll(500, arbSecretProvider) { provider ->
        val secret = provider()
        secret.base32Encoded.forEach {
          it shouldBeInRange (' '..'z')
        }
        secret.value.size shouldBe 20
      }
    }
  }
})

val arbBlockingAsciiRangeSecretProvider: Arb<() -> TOTPSecret> = Arb.constant { AsciiRangeSecretProvider().generateSecret() }
val arbAsyncAsciiRangeSecretProvider: Arb<() -> TOTPSecret> = Arb.constant { AsyncAsciiRangeSecretProvider().generateSecret().get() }
val arbCPSAsciiRangeSecretProvider: Arb<() -> TOTPSecret> = Arb.constant { runBlocking { CPSAsciiRangeSecretProvider().generateSecret() } }

val arbAsciiRangeSecretProvider: Arb<() -> TOTPSecret> =
  Arb.choice(
    arbBlockingAsciiRangeSecretProvider,
    arbAsyncAsciiRangeSecretProvider,
    arbCPSAsciiRangeSecretProvider,
  )

val arbBlockingRandomSecretProvider: Arb<() -> TOTPSecret> = Arb.constant { RandomSecretProvider().generateSecret() }
val arbAsyncRandomSecretProvider: Arb<() -> TOTPSecret> = Arb.constant { AsyncRandomSecretProvider().generateSecret().get() }
val arbCPSRandomSecretProvider: Arb<() -> TOTPSecret> = Arb.constant { runBlocking { CPSRandomSecretProvider().generateSecret() } }

val arbRandomSecretProvider: Arb<() -> TOTPSecret> =
  Arb.choice(
    arbBlockingRandomSecretProvider,
    arbAsyncRandomSecretProvider,
    arbCPSRandomSecretProvider,
  )

val arbSecretProvider: Arb<() -> TOTPSecret> =
  Arb.choice(
    arbRandomSecretProvider,
    arbAsciiRangeSecretProvider,
  )
