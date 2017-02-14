package com.thenewmotion.ocpi
package msgs

import v2_1.CommonTypes.{CountryCode, PartyId, Url}
import v2_1.Credentials.OurToken

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
    countryCode: CountryCode,
    partyId: PartyId,
    token: OurToken,
    url: Url
  )

  case class VersionDetails(
    version: VersionNumber,
    endpoints: Iterable[Endpoint]
  )

  sealed trait VersionNumber extends Nameable
  object VersionNumber extends Enumerable[VersionNumber] {
    case object `2.0` extends VersionNumber {val name = "2.0"}
    case object `2.1` extends VersionNumber {val name = "2.1"}
    val values = Set(`2.0`, `2.1`)
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

    val values = Set(Locations, Credentials, Versions, Tariffs, Tokens, Cdrs, Sessions, Commands)
  }
}
