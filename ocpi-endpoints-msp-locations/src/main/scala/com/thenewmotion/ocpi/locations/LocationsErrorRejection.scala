package com.thenewmotion.ocpi.locations

import akka.http.scaladsl.server.Rejection

case class LocationsErrorRejection(error: LocationsError) extends Rejection
