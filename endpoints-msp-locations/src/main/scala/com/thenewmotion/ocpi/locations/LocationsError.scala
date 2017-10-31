package com.thenewmotion.ocpi
package locations

sealed trait LocationsError {def reason: Option[String]}

object LocationsError {
  case class LocationNotFound(reason: Option[String] = None) extends LocationsError
  case class LocationCreationFailed(reason: Option[String] = None) extends LocationsError
  case class LocationUpdateFailed(reason: Option[String] = None) extends LocationsError
  case class EvseNotFound(reason: Option[String] = None) extends LocationsError
  case class EvseCreationFailed(reason: Option[String] = None) extends LocationsError
  case class ConnectorNotFound(reason: Option[String] = None) extends LocationsError
  case class ConnectorCreationFailed(reason: Option[String] = None) extends LocationsError
  case class IncorrectLocationId(reason: Option[String] = None) extends LocationsError
}
