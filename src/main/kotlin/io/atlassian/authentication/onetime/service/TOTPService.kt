package io.atlassian.authentication.onetime.service

import io.atlassian.authentication.onetime.core.TOTP
import io.atlassian.authentication.onetime.core.TOTPGenerator
import io.atlassian.authentication.onetime.model.EmailAddress
import io.atlassian.authentication.onetime.model.Issuer
import io.atlassian.authentication.onetime.model.TOTPSecret
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock

interface TOTPService {

  fun generateTotpSecret(): TOTPSecret

  fun generateTOTPUrl(
    totpSecret: TOTPSecret,
    emailAddress: EmailAddress,
    issuer: Issuer
  ): URI

  fun verify(
    code: TOTP,
    totpSecret: TOTPSecret
  ): TOTPVerificationResult
}

data class TOTPConfiguration(
  val secretProvider: SecretProvider = AsciiRangeSecretProvider(),
  val allowedPastSteps: Int = 0,
  val allowedFutureSteps: Int = 0
)

sealed class TOTPVerificationResult {
  object InvalidTotp : TOTPVerificationResult()
  data class Success(val index: Int) : TOTPVerificationResult()
}

class DefaultTOTPService(
  private val totpGenerator: TOTPGenerator = TOTPGenerator(clock = Clock.systemUTC()),
  private val totpConfiguration: TOTPConfiguration = TOTPConfiguration()
) : TOTPService {

  override fun generateTotpSecret(): TOTPSecret = totpConfiguration.secretProvider.generateSecret()

  override fun generateTOTPUrl(
    totpSecret: TOTPSecret,
    emailAddress: EmailAddress,
    issuer: Issuer
  ): URI {
    val encodedEmailAddress: String = URLEncoder.encode(emailAddress.value, StandardCharsets.UTF_8)
    val encodedIssuer: String = URLEncoder.encode(issuer.value, StandardCharsets.UTF_8)
    val template = "otpauth://totp/$encodedIssuer:$encodedEmailAddress?" +
      "secret=${totpSecret.base32Encoded}" +
      "&issuer=$encodedIssuer" +
      "&algorithm=${totpGenerator.digest.toQueryParam()}" +
      "&digits=${totpGenerator.otpLength.value}" +
      "&period=${totpGenerator.timeStepSeconds}"
    return URI(template)
  }

  override fun verify(
    code: TOTP,
    totpSecret: TOTPSecret
  ): TOTPVerificationResult {
    val index = totpGenerator.generate(totpSecret, totpConfiguration.allowedPastSteps, totpConfiguration.allowedFutureSteps).indexOf(code)
    return if (index == -1) {
      TOTPVerificationResult.InvalidTotp
    } else {
      TOTPVerificationResult.Success(index - totpConfiguration.allowedPastSteps)
    }
  }
}

