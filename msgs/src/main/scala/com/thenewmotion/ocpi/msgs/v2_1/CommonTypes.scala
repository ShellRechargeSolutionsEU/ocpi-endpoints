package com.thenewmotion.ocpi
package msgs
package v2_1

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

  case class BusinessDetails(
    name: String,
    logo: Option[Image],
    website: Option[Url]
  )

}
