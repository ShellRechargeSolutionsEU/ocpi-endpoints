package com.thenewmotion.ocpi.locations

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.thenewmotion.ocpi.common.{ClientError, OcpiClient}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Page
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import com.thenewmotion.time.Imports.{DateTime, ISODateTimeFormat}

class LocationsClient(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  val formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC

  def getLocations(uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext): Future[ClientError \/ Iterable[Location]] = {

    val query: Map[String, String] = Map.empty ++
      dateFrom.map("date_from" -> formatterNoMillis.print(_)) ++
      dateTo.map("date_to" -> formatterNoMillis.print(_))

    traversePaginatedResource(uri, auth, query)(res => Unmarshal(res.entity).to[Page[Location]])
  }

}

