package com.thenewmotion.ocpi.locations

import spray.routing.Rejection

case class LocationsErrorRejection(error: LocationsError) extends Rejection
