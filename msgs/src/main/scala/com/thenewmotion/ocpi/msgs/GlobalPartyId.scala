package com.thenewmotion.ocpi.msgs

import java.util.Locale

trait GlobalPartyId {
  def countryCode: String
  def partyId: String

  override def toString = countryCode + partyId
}

object GlobalPartyId {
  private val Regex = """([A-Z]{2})([A-Z0-9]{3})""".r

  private lazy val isoCountries = Locale.getISOCountries

  private case class Impl(
    countryCode: String,
    partyId: String
  ) extends GlobalPartyId

  def apply(countryCode: String, partyId: String): GlobalPartyId =
    apply(countryCode + partyId)

  def apply(globalPartyId: String): GlobalPartyId =
    globalPartyId.toUpperCase match {
      case Regex(countryCode, partyId) if isoCountries.contains(countryCode) =>
        Impl(countryCode, partyId)

      case _ => throw new IllegalArgumentException(s"$globalPartyId is not a valid global party id;  " +
        s"must be an ISO 3166-1 alpha-2 country code, followed by a party id of 3 ASCII letters or digits")
    }

  def unapply(globalPartyId: GlobalPartyId): Option[(String, String)] =
    Some((globalPartyId.countryCode, globalPartyId.partyId))

}
