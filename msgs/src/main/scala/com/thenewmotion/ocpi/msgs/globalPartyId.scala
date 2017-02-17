package com.thenewmotion.ocpi.msgs

import java.util.Locale

private trait PartyId extends Any {
  def value: String
  override def toString = value
}

private case class PartyIdImpl(value: String) extends AnyVal with PartyId

private object PartyId {
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

private trait CountryCode extends Any {
  def value: String
  override def toString = value
}

private case class CountryCodeImpl(value: String) extends AnyVal with CountryCode

private object CountryCode {
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

trait GlobalPartyId {
  def countryCode: String
  def partyId: String

  override def toString = countryCode + partyId
}

private case class GlobalPartyIdImpl(
  _countryCode: CountryCode,
  _partyId: PartyId
) extends GlobalPartyId {
  override def countryCode = _countryCode.value
  override def partyId = _partyId.value
}

object GlobalPartyId {
  def apply(countryCode: String, partyId: String): GlobalPartyId =
    GlobalPartyIdImpl(CountryCode(countryCode), PartyId(partyId))

  def apply(globalPartyId: String): GlobalPartyId =
    (GlobalPartyId.apply(_: String, _: String)).tupled(globalPartyId.splitAt(2))
}
