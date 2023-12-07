package com.atlassian.onetime

import com.atlassian.onetime.core.HMACDigest
import com.atlassian.onetime.core.OTPLength
import com.atlassian.onetime.model.EmailAddress
import com.atlassian.onetime.model.EmailDomain
import com.atlassian.onetime.model.Issuer
import com.atlassian.onetime.model.TOTPSecret
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.ascii
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.merge
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.withEdgecases
import io.kotest.property.exhaustive.collection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Arb.Companion.alpha(): Arb<Codepoint> = Arb.of((('a'..'z') + ('A'..'Z')).map { Codepoint(it.code) })

fun Arb.Companion.numeric(): Arb<Codepoint> = Arb.of(('0'..'9').map { Codepoint(it.code) })

fun Arb.Companion.alphaNumeric(): Arb<Codepoint> = Arb.alpha().merge(Arb.numeric())

val arbFullRangeTotpSecret: Arb<TOTPSecret> = Arb.byteArray(Arb.constant(20), Arb.byte()).map(::TOTPSecret)
val arbAsciiTotpSecret: Arb<TOTPSecret> = Arb.string(20, Codepoint.ascii()).map { it.toByteArray() }.map(::TOTPSecret)
val arbTotpSecret: Arb<TOTPSecret> =
  Arb.choose(
    50 to arbFullRangeTotpSecret,
    50 to arbAsciiTotpSecret,
  )

val arbOtpLength: Gen<OTPLength> =
  Exhaustive.collection(
    listOf(
      OTPLength.SIX,
      OTPLength.SEVEN,
      OTPLength.EIGHT,
      OTPLength.NINE,
      OTPLength.TEN,
    ),
  )

val arbHMACDigest: Gen<HMACDigest> =
  Exhaustive.collection(
    listOf(
      HMACDigest.SHA1,
      HMACDigest.SHA256,
      HMACDigest.SHA512,
    ),
  )
val arbDateNext20Years: Arb<LocalDateTime> = Arb.localDateTime(LocalDate.now().year, LocalDate.now().year + 20)

val arbDateAroundUnixEpoch =
  Arb.localDateTime(
    minLocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0),
    maxLocalDateTime = LocalDateTime.of(1970, 2, 1, 0, 0),
  )

val arbDateForIntOverflows =
  Arb.localDateTime(
    minLocalDateTime = LocalDateTime.of(2038, 1, 19, 0, 0),
    maxLocalDateTime = LocalDateTime.of(2038, 1, 20, 0, 0),
  )

val arbInstant: Arb<Instant> =
  Arb.choose(
    5 to arbDateAroundUnixEpoch,
    80 to arbDateNext20Years,
    15 to arbDateForIntOverflows,
  ).map { it.toInstant(ZoneOffset.UTC) }

val arbEmailDomain: Arb<EmailDomain> =
  run {
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

val arbEmailUsernameSpecialCharacter: Arb<Codepoint> =
  Arb.of(
    listOf('$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~', '.').map { Codepoint(it.code) },
  )

val arbUsername: Arb<String> = Arb.string(1, 64, Arb.alphaNumeric().merge(arbEmailUsernameSpecialCharacter))

val arbEmailString: Arb<String> =
  arbitrary {
    val username = arbUsername.bind()
    val domain = arbEmailDomain.bind()
    "$username@${domain.value}"
  }

val arbEmailAddress: Arb<EmailAddress> = arbitrary { EmailAddress(arbEmailString.bind()) }

private val arbLegalSeparator = Arb.of('-', '_', '/').map { Codepoint(it.code) }
private val arbLegalCodepoints = Arb.alphaNumeric().merge(arbLegalSeparator)
val arbIssuer: Arb<Issuer> = Arb.string(6..20, arbLegalCodepoints).map { Issuer(it) }

val arbTimeStep = Arb.int(30..90)
val arbSteps = Arb.int(0..3)
