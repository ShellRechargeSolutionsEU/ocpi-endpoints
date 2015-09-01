package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi.ListError

import scalaz.\/





trait VersionsDataHandler {

  type Version = String
  type Url = String
  type VersionsMap = Map[Version, Url]

  def versionsPath: String = "versions"
  def allVersions: VersionsMap

  def versionDetails(version: Version): ListError \/ List[Endpoint]
}


