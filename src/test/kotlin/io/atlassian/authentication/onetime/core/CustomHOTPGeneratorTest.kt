package io.atlassian.authentication.onetime.io.atlassian.authentication.onetime.core

import io.atlassian.authentication.onetime.arbHMACDigest
import io.atlassian.authentication.onetime.arbOtpLength
import io.atlassian.authentication.onetime.arbTotpSecret
import io.atlassian.authentication.onetime.core.CustomHOTPGenerator
import io.atlassian.authentication.onetime.core.HMACDigest
import io.atlassian.authentication.onetime.core.OTPLength
import io.atlassian.authentication.onetime.model.TOTPSecret
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.time.Clock

class CustomHOTPGeneratorTest : FunSpec() {
    init {
        PropertyTesting.defaultIterationCount = 1_000_000
        context("HOTP generation") {
            test("should be represented by strings of the specified length") {
                checkAll(
                    arbOtpLength,
                    arbTotpSecret,
                    arbHMACDigest,
                    Arb.long(0..Long.MAX_VALUE)
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
                    Arb.long(0..Long.MAX_VALUE)
                ) { otpLength, secret, digest, counter ->
                    given(TestState(otpLength = otpLength, digestSpecification = digest)) {
                        hotpGenerator.generate(secret.value, counter).value.toInt() shouldBeGreaterThanOrEqual 0
                    }
                }
            }

            test("should generate HOTPs defined in RFC 4226 test cases") {
                //See https://datatracker.ietf.org/doc/html/rfc4226#page-32
                val key = TOTPSecret.fromBase32EncodedString("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
                given(TestState(otpLength = OTPLength.SIX, digestSpecification = HMACDigest.SHA1)) {
                    val generatedOtps = (0L..9L).map { counter ->
                        hotpGenerator.generate(key.value, counter).value
                    }
                    generatedOtps shouldContainExactly listOf(
                        "755224",
                        "287082",
                        "359152",
                        "969429",
                        "338314",
                        "254676",
                        "287922",
                        "162583",
                        "399871",
                        "520489"
                    )
                }
            }

            test("should generate HOTPs defined in RFC 6238 test cases") {
                //See https://datatracker.ietf.org/doc/html/rfc6238#appendix-B
                val sha1Key = TOTPSecret.fromBase32EncodedString("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
                val sha256Key = TOTPSecret.fromBase32EncodedString(
                    "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
                            "GEZDGNBVGY3TQOJQGEZA"
                )
                val sha512Key = TOTPSecret.fromBase32EncodedString(
                    "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
                            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA"
                )


                val expectedResults = mapOf(
                    // key      counter.        Digest                  Expected OTP
                    sha1Key     to (1L          to (HMACDigest.SHA1     to "94287082")),
                    sha256Key   to (1L          to (HMACDigest.SHA256   to "46119246")),
                    sha512Key   to (1L          to (HMACDigest.SHA512   to "90693936")),
                    sha1Key     to (37037036L   to (HMACDigest.SHA1     to "07081804")),
                    sha256Key   to (37037036L   to (HMACDigest.SHA256   to "68084774")),
                    sha512Key   to (37037036L   to (HMACDigest.SHA512   to "25091201")),
                    sha1Key     to (37037037L   to (HMACDigest.SHA1     to "14050471")),
                    sha256Key   to (37037037L   to (HMACDigest.SHA256   to "67062674")),
                    sha512Key   to (37037037L   to (HMACDigest.SHA512   to "99943326")),
                    sha1Key     to (41152263L   to (HMACDigest.SHA1     to "89005924")),
                    sha256Key   to (41152263L   to (HMACDigest.SHA256   to "91819424")),
                    sha512Key   to (41152263L   to (HMACDigest.SHA512   to "93441116")),
                    sha1Key     to (66666666L   to (HMACDigest.SHA1     to "69279037")),
                    sha256Key   to (66666666L   to (HMACDigest.SHA256   to "90698825")),
                    sha256Key   to (66666666L   to (HMACDigest.SHA512   to "38618901")),
                    sha1Key     to (666666666L  to (HMACDigest.SHA1     to "65353130")),
                    sha256Key   to (666666666L  to (HMACDigest.SHA256   to "77737706")),
                    sha512Key   to (666666666L  to (HMACDigest.SHA512   to "47863826")),
                )

                for (entry in expectedResults.entries) {
                    val key = entry.key
                    val counter = entry.value.first
                    val digest = entry.value.second.first
                    val expectedOtp = entry.value.second.second

                    CustomHOTPGenerator(
                        otpLength = OTPLength.EIGHT,
                        digest = digest,
                    ).generate(key = key.value, counter).value shouldBe expectedOtp
                }
            }

            context("for all digests") {
                test("should generate OTPs for different key lengths") {
                    checkAll(
                        arbOtpLength,
                        arbHMACDigest,
                        Arb.int(20..64),
                        Arb.long(0..Long.MAX_VALUE)
                    ) { otpLength, digest, keyLength, counter ->
                        val key = TOTPSecret.fromBase32EncodedString("A".repeat(keyLength))
                        CustomHOTPGenerator(
                            otpLength = otpLength,
                            digest = digest,
                        ).generate(key = key.value, counter).value.length shouldBe otpLength.value
                    }
                }
            }
        }
    }

    private suspend fun given(state: TestState = TestState(), test: suspend TestState.(CustomHOTPGenerator) -> Unit) {
        with(state) {
            test(state.hotpGenerator)
        }
    }

    data class TestState(
        val clock: Clock = Clock.systemUTC(),
        val otpLength: OTPLength = OTPLength.SIX,
        val digestSpecification: HMACDigest = HMACDigest.SHA1
    ) {
        val hotpGenerator = CustomHOTPGenerator(
            otpLength = otpLength,
            digest = digestSpecification,
        )
    }
}