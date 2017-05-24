package com.thenewmotion.ocpi
package msgs

import scala.util.{Success, Try}

object Versions {

  case class Version(version: VersionNumber, url: Url)

  case class Endpoint(identifier: EndpointIdentifier, url: Url)

  case class VersionDetails(version: VersionNumber, endpoints: Iterable[Endpoint])

  case class VersionNumber(
    major: Int,
    minor: Int,
    patch: Option[Int] = None
  ) extends Ordered[VersionNumber] {

    def compare(that: VersionNumber): Int = {
      def toNumber(v: VersionNumber) = v.major * 100 + v.minor * 10 + v.patch.getOrElse(0)
      toNumber(this) - toNumber(that)
    }

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
    val `2.1.1` = VersionNumber(2, 1, 1)
  }

  case class EndpointIdentifier(value: String) {
    override def toString = value
  }

  object EndpointIdentifier {
    val Locations = EndpointIdentifier("locations")
    val Credentials = EndpointIdentifier("credentials")
    val Versions = EndpointIdentifier("versions")
    val VersionDetails = EndpointIdentifier("version-details")
    val Tariffs = EndpointIdentifier("tariffs")
    val Tokens = EndpointIdentifier("tokens")
    val Cdrs = EndpointIdentifier("cdrs")
    val Sessions = EndpointIdentifier("sessions")
    val Commands = EndpointIdentifier("commands")
  }
}

