package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{OcpiResponse, Url, BusinessDetails}
import org.joda.time.DateTime


object Credentials {


  case class Creds(
    token: String,
    url:  Url,
    business_details: BusinessDetails
    )

  case class CredsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime,
    data: Creds
    ) extends OcpiResponse

}

