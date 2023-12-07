package com.atlassian.onetime.service

import com.atlassian.onetime.core.TOTP
import com.atlassian.onetime.core.TOTPGenerator
import com.atlassian.onetime.model.EmailAddress
import com.atlassian.onetime.model.Issuer
import com.atlassian.onetime.model.TOTPSecret
import java.net.URI
import java.net.URLEncoder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface TOTPService {
  fun generateTOTPUrl(
    totpSecret: TOTPSecret,
    emailAddress: EmailAddress,
    issuer: Issuer,
  ): URI

  fun verify(
    code: TOTP,
    totpSecret: TOTPSecret,
  ): TOTPVerificationResult
}

data class TOTPConfiguration(
  val allowedPastSteps: Int = 0,
  val allowedFutureSteps: Int = 0,
)

sealed class TOTPVerificationResult {
  object InvalidTotp : TOTPVerificationResult()

  data class Success(val index: Int) : TOTPVerificationResult()

  @OptIn(ExperimentalContracts::class)
  fun isSuccess(): Boolean {
    contract { returns(true) implies (this@TOTPVerificationResult is Success) }
    return this@TOTPVerificationResult is Success
  }

  @OptIn(ExperimentalContracts::class)
  fun isFailure(): Boolean {
    contract { returns(true) implies (this@TOTPVerificationResult is InvalidTotp) }
    return this@TOTPVerificationResult is InvalidTotp
  }
}

class DefaultTOTPService(
  private val totpGenerator: TOTPGenerator = TOTPGenerator(),
  private val totpConfiguration: TOTPConfiguration = TOTPConfiguration(),
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

  override fun generateTOTPUrl(
    totpSecret: TOTPSecret,
    emailAddress: EmailAddress,
    issuer: Issuer,
  ): URI {
    val encodedIssuer: String = issuer.value.urlEncode()
    val encodedEmailAddress: String = emailAddress.value.urlEncode()
    val template =
      "$SCHEME://$TYPE/$encodedIssuer:$encodedEmailAddress?" +
        "$SECRET_QUERY_PARAM=${totpSecret.base32Encoded}" +
        "&$ISSUER_QUERY_PARAM=$encodedIssuer" +
        "&$ALGORITHM_QUERY_PARAM=${totpGenerator.digest.toQueryParam()}" +
        "&$DIGITS_QUERY_PARAM=${totpGenerator.otpLength.value}" +
        "&$PERIOD_QUERY_PARAM=${totpGenerator.timeStepSeconds}"
    return URI(template)
  }

  override fun verify(
    code: TOTP,
    totpSecret: TOTPSecret,
  ): TOTPVerificationResult {
    val index = totpGenerator.generate(totpSecret, totpConfiguration.allowedPastSteps, totpConfiguration.allowedFutureSteps).indexOf(code)
    return if (index == -1) {
      TOTPVerificationResult.InvalidTotp
    } else {
      TOTPVerificationResult.Success(index - totpConfiguration.allowedPastSteps)
    }
  }
}

fun String.urlEncode(): String =
  URLEncoder.encode(this, Charsets.UTF_8)
    .replace("+", "%20")
    .replace("*", "%2A")
    .replace("%7E", "~")
