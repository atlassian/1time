package com.atlassian.onetime.service

import com.atlassian.onetime.core.TOTP
import com.atlassian.onetime.core.TOTPGenerator
import com.atlassian.onetime.model.EmailAddress
import com.atlassian.onetime.model.Issuer
import com.atlassian.onetime.model.TOTPSecret
import java.net.URI
import java.net.URLEncoder

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
  private val totpGenerator: TOTPGenerator = TOTPGenerator(),
  private val totpConfiguration: TOTPConfiguration = TOTPConfiguration()
) : TOTPService {

  companion object {
    private const val SCHEME = "otpauth"
    private const val TYPE = "totp"
    private const val SECRET_QUERY_PARAM = "secret"
    private const val ISSUER_QUERY_PARAM = "issuer"
    private const val ALGORITHM_QUERY_PARAM = "algorithm"
    private const val DIGITS_QUERY_PARAM = "digits"
    private const val PERIOD_QUERY_PARAM = "period"
  }
  override fun generateTotpSecret(): TOTPSecret = totpConfiguration.secretProvider.generateSecret()

  override fun generateTOTPUrl(
    totpSecret: TOTPSecret,
    emailAddress: EmailAddress,
    issuer: Issuer
  ): URI {
    val encodedIssuer: String = issuer.value.urlEncode()
    val encodedEmailAddress: String = emailAddress.value.urlEncode()
    val template = "$SCHEME://$TYPE/$encodedIssuer:$encodedEmailAddress?" +
      "$SECRET_QUERY_PARAM=${totpSecret.base32Encoded}" +
      "&$ISSUER_QUERY_PARAM=$encodedIssuer" +
      "&$ALGORITHM_QUERY_PARAM=${totpGenerator.digest.toQueryParam()}" +
      "&$DIGITS_QUERY_PARAM=${totpGenerator.otpLength.value}" +
      "&$PERIOD_QUERY_PARAM=${totpGenerator.timeStepSeconds}"
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

fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8)
  .replace("+", "%20")
  .replace("*", "%2A")
  .replace("%7E", "~")
