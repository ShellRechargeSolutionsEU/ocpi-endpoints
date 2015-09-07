package com.thenewmotion.ocpi.versions

sealed trait VersionsError
sealed trait ListDetailsError extends VersionsError
sealed trait ListAllError extends VersionsError

case object UnknownVersion extends ListDetailsError
case object NoVersionsAvailable extends ListAllError

