package io.atlassian.authentication.onetime.io.atlassian.authentication.onetime.service

import io.atlassian.authentication.onetime.*
import io.atlassian.authentication.onetime.core.CustomTOTPGenerator
import io.atlassian.authentication.onetime.io.atlassian.authentication.onetime.core.CustomTOTPGeneratorTest
import io.atlassian.authentication.onetime.service.DefaultTOTPService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.net.URI
import java.time.Clock
import java.time.ZoneOffset

class DefaultTOTPServiceTest : FunSpec({

    test("generateTotpSecret") { }


    context("Generate TOTP URI"){
        test("should generate URI correctly when issuer is present ") {
            checkAll(
                arbTotpSecret,
                arbEmailAddress,
                arbIssuer,
                arbOtpLength,
                arbHMACDigest,
                Arb.int(1..60)
            ){ secret, email, issuer, otpLength, digest, timeStep ->
                val service = DefaultTOTPService(
                    CustomTOTPGenerator(
                        clock = Clock.systemUTC(),
                        timeStepSeconds = timeStep,
                        otpLength = otpLength,
                        digest = digest
                    )
                )
                val uri = service.generateTOTPUrl(
                    secret,
                    email,
                    issuer
                )

                uri.scheme shouldBe "otpauth"
                uri.authority shouldBe "totp"
                uri.path shouldBe "/${issuer.value}:${email.value}"
                uri.queryParams() shouldContainInOrder listOf(
                    QueryParam("secret", secret.base32Encoded),
                    QueryParam("issuer", issuer.value),
                    QueryParam("algorithm", digest.toQueryParam()),
                    QueryParam("digits", otpLength.value.toString()),
                    QueryParam("period", timeStep.toString()),
                )
            }
        }
    }


    test("verify") { }
})


private data class QueryParam(val name: String, val value: String)

private fun URI.queryParams(): List<QueryParam> = splitQuery(this.query)

private fun splitQuery(query: String): List<QueryParam> =
    if (query.isEmpty()) {
        emptyList()
    } else {
        query.split("&").map { pair ->
            val (name, value) = pair.split("=", limit = 2)
            QueryParam(name, value)
        }
    }


