package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{OcpiResponse, Url, BusinessDetails}
import org.joda.time.DateTime


object Credentials {


  case class Creds(
    token: String,
    url:  Url,
    business_details: BusinessDetails,
    party_id: String,
    country_code: String
    ){
    require(party_id.length == 3)
    require(country_code.length == 2)
  }

  case class CredsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime = DateTime.now(),
    data: Creds
    ) extends OcpiResponse

}

