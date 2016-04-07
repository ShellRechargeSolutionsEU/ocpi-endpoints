package com.thenewmotion.ocpi.locations

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.locations.LocationsError._
import com.thenewmotion.ocpi.msgs.v2_0.Locations.LocationsResp
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.http._
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}

class LocationsClient(implicit refFactory: ActorRefFactory, timeout: Timeout = Timeout(20.seconds)) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  def getLocations(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[LocationsError \/ LocationsResp] = {
    val pipeline = request(auth) ~> unmarshal[LocationsResp]
    val resp = pipeline(Get(uri))

    bimap(resp) {
      case Success(locations) => \/-(locations)
      case Failure(t) =>
        logger.error(s"Failed to get locations from $uri", t)
        -\/(LocationNotFound())
    }
  }
}
