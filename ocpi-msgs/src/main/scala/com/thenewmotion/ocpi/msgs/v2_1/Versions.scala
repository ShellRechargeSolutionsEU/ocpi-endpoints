package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Url

object Versions {

  case class Version(
    version: VersionNumber,
    url:  Url
    )

  case class Endpoint(
    identifier: EndpointIdentifier,
    url: Url
    )

  case class VersionsRequest(
    partyName: String,
    countryCode: String,
    partyId: String,
    token: String,
    url: Url
  )

  case class VersionDetails(
    version: VersionNumber,
    endpoints: Iterable[Endpoint]
  ) {
    require(endpoints.exists(_.identifier == EndpointIdentifier.Credentials), "Missing credentials endpoint type details")
  }

  sealed trait VersionNumber extends Nameable
  object VersionNumber extends Enumerable[VersionNumber] {
    case object `2.0` extends VersionNumber {val name = "2.0"}
    case object `2.1` extends VersionNumber {val name = "2.1"}
    val values = Seq(`2.0`, `2.1`)
  }

  sealed trait EndpointIdentifier extends Nameable
  object EndpointIdentifier extends Enumerable[EndpointIdentifier] {
    case object Locations extends EndpointIdentifier {val name = "locations"}
    case object Credentials extends EndpointIdentifier {val name = "credentials"}
    case object Versions extends EndpointIdentifier {val name = "versions"}
    case object Tariffs extends EndpointIdentifier {val name = "tariffs"}
    case object Tokens extends EndpointIdentifier {val name = "tokens"}
    case object Cdrs extends EndpointIdentifier {val name = "cdrs"}
    case object Sessions extends EndpointIdentifier {val name = "sessions"}
    case object Commands extends EndpointIdentifier {val name = "commands"}

    val values = Iterable(Locations, Credentials, Versions, Tariffs, Tokens, Cdrs, Sessions)
  }
}

