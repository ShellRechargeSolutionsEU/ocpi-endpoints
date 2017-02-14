package com.thenewmotion.ocpi.msgs.v2_1

import CommonTypes.{BusinessDetails, CountryCode, PartyId, Url}

object Credentials {

  sealed trait Token {
    def value: String
    override def toString = value
    require(value.length <= 64)
  }
  case class OurToken(value: String) extends Token
  case class TheirToken(value: String) extends Token

  case class Creds[T <: Token](
    token: T,
    url: Url,
    businessDetails: BusinessDetails,
    partyId: PartyId,
    countryCode: CountryCode
  )
}

