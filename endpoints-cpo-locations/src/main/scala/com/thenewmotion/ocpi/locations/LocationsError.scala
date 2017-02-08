package com.thenewmotion.ocpi
package locations

sealed trait LocationsError {def reason: Option[String]}

object LocationsError {
  case class LocationNotFound(reason: Option[String] = None) extends LocationsError
  case class LocationUpdateFailed(reason: Option[String] = None) extends LocationsError
  case class EvseNotFound(reason: Option[String] = None) extends LocationsError
  case class ConnectorNotFound(reason: Option[String] = None) extends LocationsError
}
