package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.{ErrorCode, SuccessCode}
import com.github.nscala_time.time.Imports._

object CommonTypes {

  case class DisplayText(
    language: String,
    text: String
  )

  sealed trait ImageCategory extends Nameable
  object ImageCategory extends Enumerable[ImageCategory] {
    case object Charger extends ImageCategory {val name = "CHARGER"}
    case object Entrance extends ImageCategory {val name = "ENTRANCE"}
    case object Location extends ImageCategory {val name = "LOCATION"}
    case object Network extends ImageCategory {val name = "NETWORK"}
    case object Operator extends ImageCategory {val name = "OPERATOR"}
    case object Other extends ImageCategory {val name = "OTHER"}
    case object Owner extends ImageCategory {val name = "OWNER"}
    val values = Iterable(Charger, Entrance, Location, Network, Operator, Other, Owner)
  }

  case class Image(
    url: Url,
    category: ImageCategory,
    `type`: String,
    width: Option[Int] = None,
    height: Option[Int] = None,
    thumbnail: Option[Url] = None
  )

  type Url = String
  case class BusinessDetails(
    name: String,
    logo: Option[Image],
    website: Option[Url]
    )

  trait PartyId extends Any {
    def value: String
    override def toString = value
  }

  private case class PartyIdImpl(value: String) extends AnyVal with PartyId

  object PartyId {
    def apply(value: String): PartyId = {
      require(value.length == 3, "PartyId must have a length of 3")
      PartyIdImpl(value)
    }
  }

  trait CountryCode extends Any {
    def value: String
    override def toString = value
  }

  private case class CountryCodeImpl(value: String) extends AnyVal with CountryCode

  object CountryCode {
    def apply(value: String): CountryCode = {
      require(value.length == 2, "CountryCode must have a length of 2")
      CountryCodeImpl(value)
    }
  }

  case class GlobalPartyId(
    countryCode: CountryCode,
    partyId: PartyId
  )

  trait OcpiResponse[Code <: OcpiStatusCode] {
    def statusCode: Code
    def statusMessage: Option[String]
    def timestamp: DateTime
  }

  case class ErrorResp(
    statusCode: ErrorCode,
    statusMessage: Option[String] = None,
    timestamp: DateTime = DateTime.now
  ) extends OcpiResponse[ErrorCode]

  trait SuccessResponse extends OcpiResponse[SuccessCode]

  case class SuccessResp(
    statusCode: SuccessCode,
    statusMessage: Option[String] = None,
    timestamp: DateTime = DateTime.now
  ) extends SuccessResponse

  case class SuccessWithDataResp[D](
    statusCode: SuccessCode,
    statusMessage: Option[String] = None,
    timestamp: DateTime = DateTime.now,
    data: D
  ) extends SuccessResponse
}