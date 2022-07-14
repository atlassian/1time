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
        totpSecret: TOTPSecret
    ): TOTPVerificationResult
}

sealed class TOTPVerificationResult {
    object InvalidTotp : TOTPVerificationResult()
    data class Success(val index: Int) : TOTPVerificationResult()
}

class DefaultTOTPService(
    private val totpGenerator: TOTPGenerator = CustomTOTPGenerator(clock = Clock.systemUTC()),
    private val secretProvider: SecretProvider = AsciiRangeSecretProvider()
) : TOTPService {

    override suspend fun generateTotpSecret(): TOTPSecret = secretProvider.generateSecret()

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
        totpSecret: TOTPSecret
    ): TOTPVerificationResult {
        val index = totpGenerator.generate(totpSecret, 0, 0).indexOf(code)
        return if (index == -1) {
            TOTPVerificationResult.InvalidTotp
        } else {
            TOTPVerificationResult.Success(index)
        }
    }
}