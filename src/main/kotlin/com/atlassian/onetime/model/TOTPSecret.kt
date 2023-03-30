package com.atlassian.onetime.model

import org.apache.commons.codec.binary.Base32

data class TOTPSecret(val value: ByteArray) {

  val base32Encoded: String = Base32().encodeToString(this.value)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as TOTPSecret

    if (!value.contentEquals(other.value)) return false

    return true
  }

  override fun hashCode(): Int {
    return value.contentHashCode()
  }

  companion object {
    fun fromBase32EncodedString(value: String): TOTPSecret = TOTPSecret(Base32().decode(value))
  }
}
