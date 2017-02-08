package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{BusinessDetails, Url}

object Credentials {
  case class Creds(
    token: String,
    url:  Url,
    businessDetails: BusinessDetails,
    partyId: String,
    countryCode: String
    ){
    require(partyId.length == 3)
    require(countryCode.length == 2)
    require(token.length <= 64)
  }
}

