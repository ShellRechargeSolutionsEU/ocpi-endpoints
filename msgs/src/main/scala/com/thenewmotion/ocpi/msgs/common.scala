package com.thenewmotion.ocpi.msgs

import java.security.SecureRandom
import java.util.Locale
import Ownership.Theirs

sealed trait Ownership

object Ownership {
  trait Theirs extends Ownership
  trait Ours extends Ownership
}

case class AuthToken[O <: Ownership](value: String) {
  override def toString = value
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

trait PartyId extends Any {
  def value: String
  override def toString = value
}

private case class PartyIdImpl(value: String) extends AnyVal with PartyId

object PartyId {
  val Regex = """([A-Za-z0-9]{3})""".r

  def isValid(id: String): Boolean = id match {
    case Regex(_) => true
    case _ => false
  }

  def apply(id: String): PartyId =
    if (isValid(id)) {
      PartyIdImpl(id.toUpperCase)
    } else throw new IllegalArgumentException(
      "PartyId must have a length of 3 and be ASCII letters or digits")

}

trait CountryCode extends Any {
  def value: String
  override def toString = value
}

private case class CountryCodeImpl(value: String) extends AnyVal with CountryCode

object CountryCode {
  val Regex = """([A-Za-z]{2})""".r

  lazy val isoCountries = Locale.getISOCountries

  def isValid(countryCode: String): Boolean = countryCode match {
    case Regex(_) if isoCountries.contains(countryCode.toUpperCase) => true
    case _ => false
  }

  def apply(countryCode: String): CountryCode =
    if (isValid(countryCode)) {
      CountryCodeImpl(countryCode.toUpperCase)
    } else throw new IllegalArgumentException(
      "Country Code must be valid according to ISO 3166-1 alpha-2")
}

case class GlobalPartyId(
  countryCode: CountryCode,
  partyId: PartyId
) {
  override def toString = s"$countryCode-$partyId"
}
