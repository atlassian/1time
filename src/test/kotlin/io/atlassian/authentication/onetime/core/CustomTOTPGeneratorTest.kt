package io.atlassian.authentication.onetime.io.atlassian.authentication.onetime.core

import io.atlassian.authentication.onetime.arbInstant
import io.atlassian.authentication.onetime.arbOtpLength
import io.atlassian.authentication.onetime.arbTotpSecret
import io.atlassian.authentication.onetime.core.CustomTOTPGenerator
import io.atlassian.authentication.onetime.core.HMACDigest
import io.atlassian.authentication.onetime.core.OTPLength
import io.atlassian.authentication.onetime.core.TOTP
import io.atlassian.authentication.onetime.model.TOTPSecret
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CustomTOTPGeneratorTest : FunSpec() {
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
                    Arb.int(0..5)
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
                    Arb.int(-5..0)
                ) { time, secret, pastSteps, futureSteps ->
                    given(TestState(clock = Clock.fixed(time, ZoneOffset.UTC))) {
                        totpGenerator.generate(secret, pastSteps, futureSteps).size shouldBe 1
                    }
                }
            }

            test("should be represented by strings of the specified length") {
                checkAll(
                    arbOtpLength,
                    arbTotpSecret
                ) { otpLength, secret->
                    given(TestState(otpLength = otpLength)) {
                        totpGenerator.generate(secret).forEach {
                            it.value.length shouldBe otpLength.value
                            it.value shouldMatch """\d{${otpLength.value}}""".toRegex()
                        }
                    }
                }
            }

            test("should generate TOTPs defined in RFC 6238 test cases") {
                //See https://datatracker.ietf.org/doc/html/rfc6238#appendix-B
                val sha1Key = TOTPSecret.fromBase32EncodedString("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
                val sha256Key = TOTPSecret.fromBase32EncodedString(
                        "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA"
                        )
                val sha512Key = TOTPSecret.fromBase32EncodedString(
                        "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZ" +
                         "DGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA"
                        )

                val expectedResults = mapOf(
                    // key      epoch time(s)       Digest                  Expected OTP
                    sha1Key     to (59L             to (HMACDigest.SHA1     to "94287082")),
                    sha256Key   to (59L             to (HMACDigest.SHA256   to "46119246")),
                    sha512Key   to (59L             to (HMACDigest.SHA512   to "90693936")),
                    sha1Key     to (1111111109L     to (HMACDigest.SHA1     to "07081804")),
                    sha256Key   to (1111111109L     to (HMACDigest.SHA256   to "68084774")),
                    sha512Key   to (1111111109L     to (HMACDigest.SHA512   to "25091201")),
                    sha1Key     to (1111111111L     to (HMACDigest.SHA1     to "14050471")),
                    sha256Key   to (1111111111L     to (HMACDigest.SHA256   to "67062674")),
                    sha512Key   to (1111111111L     to (HMACDigest.SHA512   to "99943326")),
                    sha1Key     to (1234567890L     to (HMACDigest.SHA1     to "89005924")),
                    sha256Key   to (1234567890L     to (HMACDigest.SHA256   to "91819424")),
                    sha512Key   to (1234567890L     to (HMACDigest.SHA512   to "93441116")),
                    sha1Key     to (2000000000L     to (HMACDigest.SHA1     to "69279037")),
                    sha256Key   to (2000000000L     to (HMACDigest.SHA256   to "90698825")),
                    sha256Key   to (2000000000L     to (HMACDigest.SHA512   to "38618901")),
                    sha1Key     to (20000000000L    to (HMACDigest.SHA1     to "65353130")),
                    sha256Key   to (20000000000L    to (HMACDigest.SHA256   to "77737706")),
                    sha512Key   to (20000000000L    to (HMACDigest.SHA512   to "47863826")),
                )

                for( entry in expectedResults.entries){

                    val key = entry.key
                    val time = Clock.fixed(Instant.ofEpochSecond(entry.value.first), ZoneOffset.UTC)
                    val digest = entry.value.second.first
                    val expectedOtp = entry.value.second.second

                    CustomTOTPGenerator(
                        otpLength = OTPLength.EIGHT,
                        digest = digest,
                        startTime = 0,
                        clock = time,
                        timeStepSeconds = 30
                    ).generate(key) shouldContain TOTP(expectedOtp)
                }
            }
        }
    }

    private suspend fun given(state: TestState = TestState(), test: suspend TestState.(CustomTOTPGenerator) -> Unit) {
        with(state) {
            test(state.totpGenerator)
        }
    }

    data class TestState(
        val clock: Clock = Clock.systemUTC(),
        val timeStep: Int = 30,
        val otpLength: OTPLength = OTPLength.SIX,
        val digestSpecification: HMACDigest = HMACDigest.SHA1
    ) {

        val totpGenerator = CustomTOTPGenerator(
            startTime = 0,
            timeStepSeconds = timeStep,
            otpLength = otpLength,
            digest = digestSpecification,
            clock = clock
        )
    }
}