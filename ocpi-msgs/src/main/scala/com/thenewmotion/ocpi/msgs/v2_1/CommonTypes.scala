package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.time.Imports._
import org.joda.time.DateTime

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
    val values = List(Charger, Entrance, Location, Network, Operator, Other, Owner)
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

  private[ocpi] trait OcpiResponse[StatusMessage] {
    def statusCode: Int
    def statusMessage: StatusMessage
    def timestamp: DateTime
  }

  case class ErrorResp(
    statusCode    : Int,
    statusMessage: String,
    timestamp     : DateTime = DateTime.now()
    ) extends OcpiResponse[String] {
    require(statusCode >= 2000 && statusCode <= 3999)
  }

  private[ocpi] trait SuccessResponse extends OcpiResponse[Option[String]] {
    require(statusCode >= 1000 && statusCode <= 1999)
  }

  case class SuccessResp(
    statusCode    : Int,
    statusMessage: Option[String] = None,
    timestamp     : DateTime = DateTime.now()
  ) extends SuccessResponse
}
