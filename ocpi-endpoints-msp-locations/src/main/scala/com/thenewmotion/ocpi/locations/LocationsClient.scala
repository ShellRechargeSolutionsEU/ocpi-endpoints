package com.thenewmotion.ocpi.locations

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.locations.LocationsError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.SuccessWithDataResp
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import spray.client.pipelining._
import spray.http._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}
import spray.httpx.SprayJsonSupport._

class LocationsClient(implicit refFactory: ActorRefFactory, timeout: Timeout = Timeout(20.seconds)) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def getLocations(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[LocationsError \/ SuccessWithDataResp[List[Location]]] = {
    val pipeline = request(auth) ~> unmarshal[SuccessWithDataResp[List[Location]]]
    val resp = pipeline(Get(uri))

    bimap(resp) {
      case Success(locations) => \/-(locations)
      case Failure(t) =>
        logger.error(s"Failed to get locations from $uri. Reason: ${t.getLocalizedMessage}", t)
        -\/(LocationNotFound())
    }
  }
}