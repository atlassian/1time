package io.atlassian.authentication.onetime.core

import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.math.pow

interface HOTPGenerator {

    val digest: HMACDigest

    val otpLength: OTPLength

    fun generate(key: ByteArray, counter: Long): HOTP
}

/**
 * RFC 4226 implementation of generic HOTP tokens.
 * This is core and valid for both TOTP and counter based OTP.
 */
open class CustomHOTPGenerator(
    override val otpLength: OTPLength = OTPLength.SIX,
    override val digest: HMACDigest = HMACDigest.SHA1
) : HOTPGenerator {

 
    private val modulusOperand: Int by lazy {
        10.0.pow(otpLength.value).toInt()
    }

    override fun generate(key: ByteArray, counter: Long): HOTP {
        val hmacOut = HmacUtils
            .getInitializedMac(digest.value, key)
            .doFinal(counter.toByteArray())
        val intOtp = truncate(hmacOut).rem(modulusOperand)
        return HOTP(String.format("%0${otpLength.value}d", intOtp))
    }

    private fun truncate(hmac: ByteArray): Int =
        with(hmac.last().and(0xf).toInt()) {
            ByteBuffer.wrap(
                hmac.slice(this..this + 3).toByteArray().apply {
                    set(0, get(0).and(0x7f))
                }
            ).int
        }
}

private fun Long.toByteArray(): ByteArray = ByteBuffer.allocate(8).putLong(0, this).array()


data class HOTP(val value: String)

enum class HMACDigest(val value: HmacAlgorithms) {
    SHA1(HmacAlgorithms.HMAC_SHA_1),
    SHA256(HmacAlgorithms.HMAC_SHA_256),
    SHA512(HmacAlgorithms.HMAC_SHA_512);
}

enum class OTPLength(val value: Int) {
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10)
}
