package com.thenewmotion.ocpi.locations

import akka.actor.ActorRefFactory
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.locations.Errors._
import com.thenewmotion.ocpi.msgs.v2_0.Locations.LocationResp
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.http._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{\/-, -\/, \/}

class LocationsClient(implicit refFactory: ActorRefFactory) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  def getLocations(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[LocationsError \/ LocationResp] = {
    val pipeline = request(auth) ~> unmarshal[LocationResp]
    val resp = pipeline(Get(uri))

    bimap(resp) {
      case Success(locations) => \/-(locations)
      case Failure(t) =>
        logger.error(s"Failed to get locations from $uri", t)
        -\/(LocationsRetrievalFailed)
    }
  }
}