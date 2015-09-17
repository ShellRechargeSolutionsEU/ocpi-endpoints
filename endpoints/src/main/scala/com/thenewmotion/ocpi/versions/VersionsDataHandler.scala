package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi.{Enumerable, Nameable}

import scalaz.\/



case class Endpoint(
  endpointType: EndpointType,
  version: String,
  url:  String
  )

sealed trait EndpointType extends Nameable
object EndpointTypeEnum extends Enumerable[EndpointType] {
  case object Locations extends EndpointType {val name = "locations"}
  case object Credentials extends EndpointType {val name = "credentials"}

  val values = List(Locations, Credentials)
}


trait VersionsDataHandler {

  type Version = String
  type Url = String
  type VersionsMap = Map[Version, Url]

  def versionsPath: String = "versions"
  def allVersions: ListAllError \/ VersionsMap

  def versionDetails(version: Version): ListDetailsError \/ List[Endpoint]
}


