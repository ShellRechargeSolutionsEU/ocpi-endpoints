package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.{ErrorCode, SuccessCode}
import com.thenewmotion.time.Imports._

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

  private[ocpi] trait OcpiResponse[Code <: OcpiStatusCode] {
    def statusCode: Code
    def statusMessage: Option[String]
    def timestamp: DateTime
  }

  case class ErrorResp(
    statusCode: ErrorCode,
    statusMessage: Option[String] = None,
    timestamp: DateTime = DateTime.now
  ) extends OcpiResponse[ErrorCode]

  private[ocpi] trait SuccessResponse extends OcpiResponse[SuccessCode]

  case class OcpiEnvelope(
    statusCode: OcpiStatusCode,
    statusMessage: Option[String] = None,
    timestamp: DateTime = DateTime.now
  ) extends OcpiResponse[OcpiStatusCode]

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

  case class Page[T](data: Iterable[T]) {
    def items = data
  }
}
