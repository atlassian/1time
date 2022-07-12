package io.atlassian.authentication.onetime

import io.atlassian.authentication.onetime.core.HMACDigest
import io.atlassian.authentication.onetime.core.OTPLength
import io.atlassian.authentication.onetime.model.TOTPSecret
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.*
import io.kotest.property.exhaustive.collection
import java.time.Instant
import java.time.ZoneOffset

val arbTotpSecret: Arb<TOTPSecret> = Arb.byteArray(Arb.constant(20), Arb.byte()).map(TOTPSecret::encode)

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

val arbInstant: Arb<Instant> = Arb.localDateTime(1970, 2100).map { it.toInstant(ZoneOffset.UTC) }