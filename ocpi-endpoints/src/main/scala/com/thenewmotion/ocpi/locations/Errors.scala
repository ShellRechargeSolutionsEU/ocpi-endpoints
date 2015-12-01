package com.thenewmotion.ocpi.locations

object Errors{
  sealed trait LocationsError

  case object LocationsRetrievalFailed extends LocationsError

}