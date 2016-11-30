package com.thenewmotion.ocpi.locations

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi.common.{ClientError, OcpiClient}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Page
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import com.thenewmotion.time.Imports.{DateTime, ISODateTimeFormat}
import spray.http.Uri

class LocationsClient(implicit refFactory: ActorRefFactory, timeout: Timeout = Timeout(20.seconds)) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC

  def getLocations(uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext): Future[ClientError \/ Iterable[Location]] = {

    val query: Map[String, String] = Map.empty ++
      dateFrom.map("date_from" -> formatterNoMillis.print(_)) ++
      dateTo.map("date_to" -> formatterNoMillis.print(_))

    traversePaginatedResource(uri, auth, query)(unmarshal[Page[Location]])
  }

}

