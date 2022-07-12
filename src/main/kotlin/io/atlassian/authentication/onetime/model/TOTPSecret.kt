package io.atlassian.authentication.onetime.model

import org.apache.commons.codec.binary.Base32

data class TOTPSecret private constructor(val base32Encoded: String ) {

    fun decode(): ByteArray = Base32().decode(this.base32Encoded)

    companion object {
        fun encode(bytes: ByteArray): TOTPSecret = fromString(Base32().encodeToString(bytes))
        fun fromString(base32Encoded: String): TOTPSecret = TOTPSecret(base32Encoded)
    }
}
