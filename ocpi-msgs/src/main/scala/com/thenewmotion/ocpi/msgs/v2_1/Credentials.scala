package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{SuccessResponse, Url, BusinessDetails}
import org.joda.time.DateTime


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

  case class CredsResp(
    statusCode: Int,
    statusMessage: Option[String],
    timestamp: DateTime = DateTime.now(),
    data: Creds
    ) extends SuccessResponse

}

