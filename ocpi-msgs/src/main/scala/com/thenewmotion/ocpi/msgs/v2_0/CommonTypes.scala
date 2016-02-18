package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.{Enumerable, Nameable}
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

  trait OcpiResponse {
    def status_code: Int
    def status_message: Option[String]
    def timestamp: DateTime
  }

  case class ErrorResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime = DateTime.now()
    ) extends OcpiResponse {
    require(status_code >= 2000 && status_code <= 3999)
  }

  case class SuccessResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime = DateTime.now()
    ) extends OcpiResponse {
    require(status_code >= 1000 && status_code <= 1999)
  }
}
