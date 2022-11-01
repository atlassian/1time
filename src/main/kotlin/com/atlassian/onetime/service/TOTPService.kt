package com.atlassian.onetime.service

import com.atlassian.onetime.core.TOTP
import com.atlassian.onetime.core.TOTPGenerator
import com.atlassian.onetime.model.EmailAddress
import com.atlassian.onetime.model.Issuer
import com.atlassian.onetime.model.TOTPSecret
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock

interface TOTPService {

  suspend fun generateTotpSecret(): TOTPSecret

  suspend fun generateTOTPUrl(
    totpSecret: TOTPSecret,
    emailAddress: EmailAddress,
    issuer: Issuer
  ): URI

  suspend fun verify(
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
  private val totpGenerator: TOTPGenerator = TOTPGenerator(),
  private val totpConfiguration: TOTPConfiguration = TOTPConfiguration()
) : TOTPService {

  override suspend  fun generateTotpSecret(): TOTPSecret = totpConfiguration.secretProvider.generateSecret()

  override suspend   fun generateTOTPUrl(
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

  override suspend fun verify(
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
