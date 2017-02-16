package com.thenewmotion.ocpi
package msgs

import scala.util.{Success, Try}

object Versions {

  case class Version(
    version: VersionNumber,
    url:  Url
    )

  case class Endpoint(
    identifier: EndpointIdentifier,
    url: Url
    )

  case class VersionDetails(
    version: VersionNumber,
    endpoints: Iterable[Endpoint]
  )

  case class VersionNumber(major: Int, minor: Int, patch: Option[Int] = None) {
    override def toString = patch.foldLeft(s"$major.$minor")(_ + "." + _)
  }

  object VersionNumber {
    def apply(major: Int, minor: Int, patch: Int): VersionNumber =
      VersionNumber(major, minor, Some(patch))

    def apply(value: String): VersionNumber =
      Try(value.split('.').map(_.toInt).toList) match {
        case Success(major :: minor :: Nil) => VersionNumber(major, minor)
        case Success(major :: minor :: patch :: Nil) => VersionNumber(major, minor, patch)
        case _ => throw new IllegalArgumentException(s"$value is not a valid version")
      }

    def opt(value: String): Option[VersionNumber] = Try(apply(value)).toOption

    val `2.0` = VersionNumber(2, 0)
    val `2.1` = VersionNumber(2, 1)
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

