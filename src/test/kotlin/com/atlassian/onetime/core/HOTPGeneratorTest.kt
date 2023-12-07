package com.atlassian.onetime.core

import com.atlassian.onetime.arbHMACDigest
import com.atlassian.onetime.arbOtpLength
import com.atlassian.onetime.arbTotpSecret
import com.atlassian.onetime.model.TOTPSecret
import io.kotest.core.Tuple4
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.time.Clock

class HOTPGeneratorTest : FunSpec() {
  init {
    context("HOTP generation") {
      test("should be represented by strings of the specified length") {
        checkAll(
          arbOtpLength,
          arbTotpSecret,
          arbHMACDigest,
          Arb.long(0..Long.MAX_VALUE),
        ) { otpLength, secret, digest, counter ->
          given(TestState(otpLength = otpLength, digestSpecification = digest)) {
            hotpGenerator.generate(secret.value, counter).run {
              value.length shouldBe otpLength.value
            }
          }
        }
      }

      test("should be always positive numbers") {
        checkAll(
          arbOtpLength,
          arbTotpSecret,
          arbHMACDigest,
          Arb.long(0..Long.MAX_VALUE),
        ) { otpLength, secret, digest, counter ->
          given(TestState(otpLength = otpLength, digestSpecification = digest)) {
            hotpGenerator.generate(secret.value, counter).value.toInt() shouldBeGreaterThanOrEqual 0
          }
        }
      }

      test("should generate HOTPs defined in RFC 4226 test cases") {
        // See https://datatracker.ietf.org/doc/html/rfc4226#page-32
        val key = TOTPSecret.fromBase32EncodedString("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        given(TestState(otpLength = OTPLength.SIX, digestSpecification = HMACDigest.SHA1)) {
          val generatedOtps =
            (0L..9L).map { counter ->
              hotpGenerator.generate(key.value, counter).value
            }
          generatedOtps shouldContainExactly
            listOf(
              "755224",
              "287082",
              "359152",
              "969429",
              "338314",
              "254676",
              "287922",
              "162583",
              "399871",
              "520489",
            )
        }
      }

      context("HOTPs defined in RFC 6238 test cases") {
        // See https://datatracker.ietf.org/doc/html/rfc6238#appendix-B
        val sha1Key = TOTPSecret.fromBase32EncodedString("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        val sha256Key =
          TOTPSecret.fromBase32EncodedString(
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
              "GEZDGNBVGY3TQOJQGEZA",
          )
        val sha512Key =
          TOTPSecret.fromBase32EncodedString(
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
              "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA",
          )

        withData(
          Tuple4(sha1Key, 1L, HMACDigest.SHA1, "94287082"),
          Tuple4(sha256Key, 1L, HMACDigest.SHA256, "46119246"),
          Tuple4(sha512Key, 1L, HMACDigest.SHA512, "90693936"),
          Tuple4(sha1Key, 37037036L, HMACDigest.SHA1, "07081804"),
          Tuple4(sha256Key, 37037036L, HMACDigest.SHA256, "68084774"),
          Tuple4(sha512Key, 37037036L, HMACDigest.SHA512, "25091201"),
          Tuple4(sha1Key, 37037037L, HMACDigest.SHA1, "14050471"),
          Tuple4(sha256Key, 37037037L, HMACDigest.SHA256, "67062674"),
          Tuple4(sha512Key, 37037037L, HMACDigest.SHA512, "99943326"),
          Tuple4(sha1Key, 41152263L, HMACDigest.SHA1, "89005924"),
          Tuple4(sha256Key, 41152263L, HMACDigest.SHA256, "91819424"),
          Tuple4(sha512Key, 41152263L, HMACDigest.SHA512, "93441116"),
          Tuple4(sha1Key, 66666666L, HMACDigest.SHA1, "69279037"),
          Tuple4(sha256Key, 66666666L, HMACDigest.SHA256, "90698825"),
          Tuple4(sha512Key, 66666666L, HMACDigest.SHA512, "38618901"),
          Tuple4(sha1Key, 666666666L, HMACDigest.SHA1, "65353130"),
          Tuple4(sha256Key, 666666666L, HMACDigest.SHA256, "77737706"),
          Tuple4(sha512Key, 666666666L, HMACDigest.SHA512, "47863826"),
        ) { (key, counter, digest, otp) ->
          HOTPGenerator(
            otpLength = OTPLength.EIGHT,
            digest = digest,
          ).generate(key = key.value, counter).value shouldBe otp
        }
      }

      context("for all digests") {
        test("should generate OTPs for different key lengths") {
          checkAll(
            arbOtpLength,
            arbHMACDigest,
            Arb.int(20..64),
            Arb.long(0..Long.MAX_VALUE),
          ) { otpLength, digest, keyLength, counter ->
            val key = TOTPSecret.fromBase32EncodedString("A".repeat(keyLength))
            HOTPGenerator(
              otpLength = otpLength,
              digest = digest,
            ).generate(key = key.value, counter).value.length shouldBe otpLength.value
          }
        }
      }
    }
  }

  private fun given(
    state: TestState = TestState(),
    test: TestState.(HOTPGenerator) -> Unit,
  ) {
    with(state) {
      test(state.hotpGenerator)
    }
  }

  data class TestState(
    val clock: Clock = Clock.systemUTC(),
    val otpLength: OTPLength = OTPLength.SIX,
    val digestSpecification: HMACDigest = HMACDigest.SHA1,
  ) {
    val hotpGenerator =
      HOTPGenerator(
        otpLength = otpLength,
        digest = digestSpecification,
      )
  }
}
