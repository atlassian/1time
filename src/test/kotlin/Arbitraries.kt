package io.atlassian.authentication.onetime

import io.atlassian.authentication.onetime.core.HMACDigest
import io.atlassian.authentication.onetime.core.OTPLength
import io.atlassian.authentication.onetime.model.EmailAddress
import io.atlassian.authentication.onetime.model.EmailDomain
import io.atlassian.authentication.onetime.model.Issuer
import io.atlassian.authentication.onetime.model.TOTPSecret
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.*
import io.kotest.property.exhaustive.collection
import java.time.Instant
import java.time.ZoneOffset

fun Arb.Companion.alpha(): Arb<Codepoint> = Arb.of((('a'..'z') + ('A'..'Z')).map { Codepoint(it.code) })
fun Arb.Companion.numeric(): Arb<Codepoint> = Arb.of(('0'..'9').map { Codepoint(it.code) })
fun Arb.Companion.alphaNumeric(): Arb<Codepoint> = Arb.alpha().merge(Arb.numeric())

val arbTotpSecret: Arb<TOTPSecret> = Arb.byteArray(Arb.constant(20), Arb.byte()).map(::TOTPSecret)

val arbOtpLength: Gen<OTPLength> = Exhaustive.collection(
    listOf(
        OTPLength.SIX,
        OTPLength.SEVEN,
        OTPLength.EIGHT,
        OTPLength.NINE,
        OTPLength.TEN
    )
)

val arbHMACDigest: Gen<HMACDigest> = Exhaustive.collection(
    listOf(
        HMACDigest.SHA1,
        HMACDigest.SHA256,
        HMACDigest.SHA512
    )
)

val arbInstant: Arb<Instant> = Arb.localDateTime(2020, 2050).map { it.toInstant(ZoneOffset.UTC) }

val arbEmailDomain: Arb<EmailDomain> = run {
    val invalidLabelStartingChars = listOf('.', '-')
    val invalidLabelEndingChars = listOf('.', '-')

    val arbDomainLabelCodepoints = Arb.of((('a'..'z') + ('A'..'Z') + ('0'..'9') + '-').map { Codepoint(it.code) })
    val arbDomainLabel: Arb<String> =
        Arb.string(3..(62 / 2), arbDomainLabelCodepoints)
            .map { label ->
                val substring = if (label.lowercase().startsWith("xn--")) "x$label" else label
                val prefix = if (invalidLabelStartingChars.contains(substring.first())) "c" else ""
                val suffix = if (invalidLabelEndingChars.contains(substring.last())) "c" else ""

                "$prefix$substring$suffix"
            }

    Arb.list(arbDomainLabel, 2..5)
        .map { it.joinToString(".") }
        .filter { domainString -> domainString.isNotEmpty() }
        .map { domainString ->
            val substring = domainString.take(252 / 2)
            if (invalidLabelEndingChars.contains(substring.last())) "${substring}c" else substring
        }
        .map { EmailDomain(it) }
        .withEdgecases(emptyList())
}

val arbEmailUsernameSpecialCharacter: Arb<Codepoint> = Arb.of(
    listOf('$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~', '.').map { Codepoint(it.code) }
)

val arbUsername: Arb<String> = Arb.string(1, 64, Arb.alphaNumeric().merge(arbEmailUsernameSpecialCharacter))

val arbEmailString: Arb<String> = arbitrary {
    val username = arbUsername.bind()
    val domain = arbEmailDomain.bind()
    "$username@${domain.value}"
}

val arbEmailAddress: Arb<EmailAddress> = arbitrary { EmailAddress(arbEmailString.bind()) }

private val arbLegalSeparator = Arb.of('-', '_', '/').map { Codepoint(it.code) }
private val arbLegalCodepoints = Arb.alphaNumeric().merge(arbLegalSeparator)
val arbIssuer: Arb<Issuer> = Arb.string(6..20, arbLegalCodepoints).map { Issuer(it) }