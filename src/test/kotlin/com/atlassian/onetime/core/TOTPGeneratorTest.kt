package com.atlassian.onetime.core

import com.atlassian.onetime.arbInstant
import com.atlassian.onetime.arbOtpLength
import com.atlassian.onetime.arbTotpSecret
import com.atlassian.onetime.model.TOTPSecret
import io.kotest.core.Tuple4
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TOTPGeneratorTest : FunSpec() {
  init {
    context("TOTP generation") {
      test("should always generate a non empty list of one element when no past or future steps are provided") {
        checkAll(arbInstant, arbTotpSecret) { time, secret ->
          given(TestState(clock = Clock.fixed(time, ZoneOffset.UTC))) {
            totpGenerator.generate(secret, 0, 0).size shouldBe 1
          }
        }
      }

      test("should always generate a non empty list of one element relying on defaults") {
        checkAll(arbInstant, arbTotpSecret) { time, secret ->
          given(TestState(clock = Clock.fixed(time, ZoneOffset.UTC))) {
            totpGenerator.generate(secret).size shouldBe 1
          }
        }
      }

      test("should generate the expected number of TOTPs") {
        checkAll(
          arbInstant,
          arbTotpSecret,
          Arb.int(0..5),
          Arb.int(0..5),
        ) { time, secret, pastSteps, futureSteps ->
          given(TestState(clock = Clock.fixed(time, ZoneOffset.UTC))) {
            val otps = totpGenerator.generate(secret, pastSteps, futureSteps)
            otps.size shouldBe pastSteps + 1 + futureSteps
          }
        }
      }

      test("should generate the expected number of TOTPs when providing negative values or zero for steps") {
        checkAll(
          arbInstant,
          arbTotpSecret,
          Arb.int(-5..0),
          Arb.int(-5..0),
        ) { time, secret, pastSteps, futureSteps ->
          given(TestState(clock = Clock.fixed(time, ZoneOffset.UTC))) {
            totpGenerator.generate(secret, pastSteps, futureSteps).size shouldBe 1
          }
        }
      }

      test("should be represented by strings of the specified length") {
        checkAll(
          arbOtpLength,
          arbTotpSecret,
        ) { otpLength, secret ->
          given(TestState(otpLength = otpLength)) {
            val totp = totpGenerator.generateCurrent(secret)
            totp.value.length shouldBe otpLength.value
            totp.value shouldMatch """\d{${otpLength.value}}""".toRegex()
          }
        }
      }

      context("TOTPs defined in RFC 6238 test cases") {
        // See https://datatracker.ietf.org/doc/html/rfc6238#appendix-B
        val sha1Key = TOTPSecret.fromBase32EncodedString("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        val sha256Key =
          TOTPSecret.fromBase32EncodedString(
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA",
          )
        val sha512Key =
          TOTPSecret.fromBase32EncodedString(
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZ" +
              "DGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA",
          )
        withData(
          Tuple4(sha1Key, 59L, HMACDigest.SHA1, "94287082"),
          Tuple4(sha256Key, 59L, HMACDigest.SHA256, "46119246"),
          Tuple4(sha512Key, 59L, HMACDigest.SHA512, "90693936"),
          Tuple4(sha1Key, 1111111109L, HMACDigest.SHA1, "07081804"),
          Tuple4(sha256Key, 1111111109L, HMACDigest.SHA256, "68084774"),
          Tuple4(sha512Key, 1111111109L, HMACDigest.SHA512, "25091201"),
          Tuple4(sha1Key, 1111111111L, HMACDigest.SHA1, "14050471"),
          Tuple4(sha256Key, 1111111111L, HMACDigest.SHA256, "67062674"),
          Tuple4(sha512Key, 1111111111L, HMACDigest.SHA512, "99943326"),
          Tuple4(sha1Key, 1234567890L, HMACDigest.SHA1, "89005924"),
          Tuple4(sha256Key, 1234567890L, HMACDigest.SHA256, "91819424"),
          Tuple4(sha512Key, 1234567890L, HMACDigest.SHA512, "93441116"),
          Tuple4(sha1Key, 2000000000L, HMACDigest.SHA1, "69279037"),
          Tuple4(sha256Key, 2000000000L, HMACDigest.SHA256, "90698825"),
          Tuple4(sha512Key, 2000000000L, HMACDigest.SHA512, "38618901"),
          Tuple4(sha1Key, 20000000000L, HMACDigest.SHA1, "65353130"),
          Tuple4(sha256Key, 20000000000L, HMACDigest.SHA256, "77737706"),
          Tuple4(sha512Key, 20000000000L, HMACDigest.SHA512, "47863826"),
        ) { (key, epoch, digest, otp) ->
          val time = Clock.fixed(Instant.ofEpochSecond(epoch), ZoneOffset.UTC)
          TOTPGenerator(
            otpLength = OTPLength.EIGHT,
            digest = digest,
            startTime = 0,
            clock = time,
            timeStepSeconds = 30,
          ).generateCurrent(key) shouldBe TOTP(otp)
        }
      }
    }
  }

  private fun given(
    state: TestState = TestState(),
    test: TestState.(TOTPGenerator) -> Unit,
  ) {
    with(state) {
      test(state.totpGenerator)
    }
  }

  data class TestState(
    val clock: Clock = Clock.systemUTC(),
    val timeStep: Int = 30,
    val otpLength: OTPLength = OTPLength.SIX,
    val digestSpecification: HMACDigest = HMACDigest.SHA1,
  ) {
    val totpGenerator =
      TOTPGenerator(
        startTime = 0,
        timeStepSeconds = timeStep,
        otpLength = otpLength,
        digest = digestSpecification,
        clock = clock,
      )
  }
}
