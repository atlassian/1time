package io.atlassian.authentication.onetime.service

import io.atlassian.authentication.onetime.core.CustomTOTPGenerator
import io.atlassian.authentication.onetime.core.HMACDigest
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

    suspend fun generateTotpSecret(): TOTPSecret

    suspend fun generateTOTPUrl(
        totpSecret: TOTPSecret,
        emailAddress: EmailAddress,
        issuer: Issuer
    ): URI

    suspend fun verify(
        code: TOTP,
        totpSecret: TOTPSecret,
        allowedPastSteps: Int = 0,
        allowedFutureSteps: Int = 0
    ): TOTPVerificationResult
}

data class TOTPConfiguration(
  val secretProvider: SecretProvider = AsciiRangeSecretProvider(),
  val allowedPastSteps: Int,
  val allowedFutureSteps: Int
)

sealed class TOTPVerificationResult {
    object InvalidTotp : TOTPVerificationResult()
    data class Success(val index: Int) : TOTPVerificationResult()
}

class DefaultTOTPService(
    private val totpGenerator: TOTPGenerator = CustomTOTPGenerator(clock = Clock.systemUTC()),
    private val totpConfiguration: TOTPConfiguration
) : TOTPService {

    override suspend fun generateTotpSecret(): TOTPSecret = totpConfiguration.secretProvider.generateSecret()

    override suspend fun generateTOTPUrl(
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
        totpSecret: TOTPSecret,
        allowedPastSteps: Int,
        allowedFutureSteps: Int
    ): TOTPVerificationResult {
        val index = totpGenerator.generate(totpSecret, allowedPastSteps, allowedFutureSteps).indexOf(code)
        return if (index == -1) {
            TOTPVerificationResult.InvalidTotp
        } else {
            TOTPVerificationResult.Success(index - allowedPastSteps)
        }
    }
}

