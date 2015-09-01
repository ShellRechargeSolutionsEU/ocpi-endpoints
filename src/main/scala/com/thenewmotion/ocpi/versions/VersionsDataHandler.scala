package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi.ListError

import scalaz.\/





trait VersionsDataHandler {

  type Version = String
  type Url = String
  type VersionsMap = Map[Version, Url]

  def versionsPath: String = "versions"
  def allVersions: ListError \/ VersionsMap

  def versionDetails(version: Version): ListError \/ List[Endpoint]
}


