package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.{OcpiClient, PaginatedSource}
import msgs.v2_1.Locations.Location
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.AuthToken

class LocationsClient(implicit http: HttpExt) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getLocations(uri: Uri, auth: AuthToken[Ours], dateFrom: Option[ZonedDateTime] = None, dateTo: Option[ZonedDateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[ErrorRespOr[Iterable[Location]]] =
    traversePaginatedResource[Location](uri, auth, dateFrom, dateTo)

  def locationsSource(uri: Uri, auth: AuthToken[Ours], dateFrom: Option[ZonedDateTime] = None, dateTo: Option[ZonedDateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Source[Location, NotUsed] =
    PaginatedSource[Location](http, uri, auth, dateFrom, dateTo)

}
