package com.thenewmotion.ocpi.msgs
package v2_1

import CommonTypes.BusinessDetails

object Credentials {

  case class Creds[T <: AuthToken](
    token: T,
    url: Url,
    businessDetails: BusinessDetails,
    partyId: PartyId,
    countryCode: CountryCode
  )
}

