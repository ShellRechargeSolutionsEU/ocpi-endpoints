package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{OcpiResponse, Url}
import com.thenewmotion.ocpi.msgs.{Enumerable, Nameable}
import org.joda.time.DateTime

object Versions {


  case class Version(
    version: String,
    url:  Url
    )

  case class Endpoint(
    identifier: EndpointIdentifier,
    url: Url
    )

  case class VersionsRequest(
    token: String,
    url: Url
  )

  case class VersionDetails(
    version: String,
    endpoints: List[Endpoint]
    )

  case class VersionDetailsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime,
    data: VersionDetails
    ) extends OcpiResponse {
    require(data.endpoints.exists(_.identifier == EndpointIdentifier.Credentials), "Missing credentials endpoint type details")
    require(data.endpoints.exists(_.identifier == EndpointIdentifier.Locations), "Missing locations endpoint type details")
  }


  case class VersionsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime,
    data: List[Version]
    ) extends OcpiResponse

  sealed trait EndpointIdentifier extends Nameable
  object EndpointIdentifier extends Enumerable[EndpointIdentifier] {
    case object Locations extends EndpointIdentifier {val name = "locations"}
    case object Credentials extends EndpointIdentifier {val name = "credentials"}

    val values = List(Locations, Credentials)
  }
}

