package io.atlassian.authentication.onetime.core

import io.atlassian.authentication.onetime.model.TOTPSecret
import java.time.Clock

data class TOTP(val value: String)

/**
 * Custom implementation of HOTP generator specifically for time-based one time password.
 * This leverages HMAC generation and truncation done generically on HOTPGenerator and extend it
 * to provide specific intricacies of TOTP generation
 */
class TOTPGenerator(
  private val clock: Clock = Clock.systemUTC(),
  val startTime: Int = 0,
  val timeStepSeconds: Int = 30,
  override val otpLength: OTPLength = OTPLength.SIX,
  override val digest: HMACDigest = HMACDigest.SHA1
) : OTPGenerator(otpLength, digest) {


  /**
   * Generates the TOTPs given the [totpSecret] with delay steps and future steps.
   * [delaySteps] (positive) number of past time steps that are to be generated
   * [futureSteps] (positive) number of future time steps that are to be generated
   *
   * Returns a list of TOTPs. This will contain all past steps (if [delaySteps] > 0),
   * followed by the current step and then possibly followed by all future TOTPs if specified.
   */
  fun generate(totpSecret: TOTPSecret, delaySteps: Int = 0, futureSteps: Int = 0): List<TOTP> {
    val step: Long = ((clock.millis() / 1000) - startTime) / timeStepSeconds
    return ((step - delaySteps until step) + step + (step + 1..step + futureSteps))
      .map { generateOtp(totpSecret.value, it) }
      .map { hotp -> TOTP(hotp.value) }
  }

  /**
   * Generates current TOTP given the [totpSecret]
   */
  fun generateCurrent(totpSecret: TOTPSecret): TOTP = generate(totpSecret, delaySteps = 0, futureSteps = 0)[0]
}
