package com.thenewmotion.ocpi.msgs

import java.security.SecureRandom
import Ownership.Theirs

sealed trait Ownership {
  type Opposite <: Ownership
}

object Ownership {
  trait Theirs extends Ownership {
    type Opposite = Ours
  }
  trait Ours extends Ownership {
    type Opposite = Theirs
  }
}

case class AuthToken[O <: Ownership](value: String) {
  override def toString = value.substring(0, 3) + "..."
  require(value.length <= 64)
}

object AuthToken {
  private val TOKEN_LENGTH = 32
  private val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  private val secureRandom = new SecureRandom()

  private def generateToken(length: Int): AuthToken[Theirs] =
    AuthToken[Theirs]((1 to length).map(_ => TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length))).mkString)

  def generateTheirs: AuthToken[Theirs] = generateToken(TOKEN_LENGTH)
}

trait CountryCode extends Any { def value: String }
object CountryCode {
  private case class Impl(value: String) extends AnyVal with CountryCode

  def apply(value: String): CountryCode = {
    require(value.length == 3, "Must be a 3-letter, ISO 3166-1 country code")
    Impl(value.toUpperCase)
  }

  def unapply(cc: CountryCode): Option[String] = Some(cc.value)
}

trait Language extends Any { def value: String }
object Language {
  private case class Impl(value: String) extends AnyVal with Language

  def apply(value: String): Language = {
    require(value.length == 2, "Must be a 2-letter, ISO 639-1 language code")
    Impl(value.toUpperCase)
  }

  def unapply(l: Language): Option[String] = Some(l.value)
}

case class Url(value: String) extends AnyVal {
  def +(other: String) = Url(value + other)
  override def toString = value
}

trait CurrencyCode extends Any { def value: String }
object CurrencyCode {
  private case class Impl(value: String) extends AnyVal with CurrencyCode

  def apply(value: String): CurrencyCode = {
    require(value.length == 3, "Must be a 3-letter, ISO 4217 Code")
    Impl(value.toUpperCase)
  }

  def unapply(cc: CurrencyCode): Option[String] = Some(cc.value)
}

