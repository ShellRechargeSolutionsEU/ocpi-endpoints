package com.thenewmotion.ocpi
package locations

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import akka.stream.ActorMaterializer
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import com.github.nscala_time.time.Imports._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp

class LocationsClient(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._


  def getLocations(uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext): Future[ErrorResp \/ Iterable[Location]] = {

    val query: Map[String, String] = Map.empty ++
      dateFrom.map("date_from" -> formatterNoMillis.print(_)) ++
      dateTo.map("date_to" -> formatterNoMillis.print(_))

    traversePaginatedResource[Location](uri, auth, query)
  }

}

