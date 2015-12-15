package com.thenewmotion.ocpi.locations

sealed trait LocationsError {def reason: Option[String]}

object LocationsError{

  case class LocationRetrievalFailed(reason: Option[String] = None) extends LocationsError
  case class LocationCreationFailed(reason: Option[String] = None) extends LocationsError
  case class EvseRetrievalFailed(reason: Option[String] = None) extends LocationsError
  case class ConnectorRetrievalFailed(reason: Option[String] = None) extends LocationsError

}