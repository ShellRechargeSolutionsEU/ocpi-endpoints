package com.thenewmotion.ocpi.locations

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi.common.{ClientError, OcpiClient}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Page
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scalaz._

class LocationsClient(implicit refFactory: ActorRefFactory, timeout: Timeout = Timeout(20.seconds)) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def getLocations(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[ClientError \/ Iterable[Location]] = {
    traversePaginatedResource(uri, auth)(unmarshal[Page[Location]])
  }

}

