package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.time.Imports._
import org.joda.time.DateTime

object CommonTypes {



  type DisplayText = List[LocalizedText]
  case class LocalizedText(
    language: Option[String],
    text: Option[String]
  )

  type Url = String
  case class BusinessDetails(
    name: String,
    logo: Option[Url],
    website: Option[Url]
    )

  trait OcpiResponse {
    def status_code: Int
    def status_message: Option[String]
    def timestamp: DateTime
  }

  case class ErrorResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime
    ) extends OcpiResponse {
    require(status_code >= 2000 && status_code <= 3999)
  }

  case class SuccessResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime
    ) extends OcpiResponse {
    require(status_code >= 1000 && status_code <= 1999)
  }
}
