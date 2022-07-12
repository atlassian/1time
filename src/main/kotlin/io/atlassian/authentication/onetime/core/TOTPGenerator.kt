package io.atlassian.authentication.onetime.core

import io.atlassian.authentication.onetime.model.TOTPSecret
import java.time.Clock

interface TOTPGenerator: HOTPGenerator{

    val startTime: Int
    val timeStepSeconds: Int

    /**
     * Generates the TOTPs given the [totpSecret] with delay steps and future steps.
     * [delaySteps] (positive) number of past time steps that are to be generated
     * [futureSteps] (positive) number of future time steps that are to be generated
     *
     * Returns a list of TOTPs. The head will always contain the current TOTP,
     * possibly followed by all past TOTPs and then all future TOTPs if specified.
     *
     */
    fun generate(totpSecret: TOTPSecret, delaySteps: Int = 0, futureSteps: Int = 0): List<TOTP>
}

/**
 * Custom implementation of HOTP generator specifically for time-based one time password.
 * This leverages HMAC generation and truncation done generically on HOTPGenerator and extend it
 * to provide specific intricacies of TOTP generation
 */
class CustomTOTPGenerator(
    private val clock: Clock,
    override val startTime: Int = 0,
    override val timeStepSeconds: Int = 30,
    override val otpLength: OTPLength = OTPLength.SIX,
    override val digest: HMACDigest = HMACDigest.SHA1
) : TOTPGenerator, CustomHOTPGenerator(otpLength, digest) {

    override fun generate(totpSecret: TOTPSecret, delaySteps: Int, futureSteps: Int): List<TOTP> {
        val decodedTotpSecret: ByteArray = totpSecret.decode()
        val step: Long = ((clock.millis() / 1000) - startTime) / timeStepSeconds
        return ((step - delaySteps until step) + step + (step + 1..step + futureSteps))
                .map {generate(decodedTotpSecret, it) }
                .map { hotp -> TOTP(hotp.value) }
    }
}


data class TOTP(val value: String)


