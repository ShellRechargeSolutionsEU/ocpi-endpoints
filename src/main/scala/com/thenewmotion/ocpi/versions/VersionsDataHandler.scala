package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi.ListError

import scalaz.\/





trait VersionsDataHandler {

  type Version = String
  type VersionDetailUrl = String
  type VersionsMap = Map[Version, VersionDetailUrl]

  def versionsNamespace: String = "versions"
  def allVersions: VersionsMap
  require(allVersions.nonEmpty)

  def versionDetails(version: Version): ListError \/ List[Endpoint]
}


