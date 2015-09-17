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
    identifier: EndpointIdentifierType,
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
    ) extends OcpiResponse


  case class VersionsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime,
    data: List[Version]
    ) extends OcpiResponse

  sealed trait EndpointIdentifierType extends Nameable
  object EndpointIdentifierEnum extends Enumerable[EndpointIdentifierType] {
    case object Locations extends EndpointIdentifierType {val name = "locations"}
    case object Credentials extends EndpointIdentifierType {val name = "credentials"}

    val values = List(Locations, Credentials)
  }
}

