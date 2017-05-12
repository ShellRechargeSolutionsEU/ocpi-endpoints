package com.thenewmotion.ocpi
package locations

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.{OcpiClient, PaginatedSource}
import msgs.v2_1.Locations.Location
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import com.github.nscala_time.time.Imports._
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.{AuthToken, ErrorResp}

class LocationsClient(implicit http: HttpExt) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getLocations(uri: Uri, auth: AuthToken[Ours], dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[ErrorResp \/ Iterable[Location]] =
    traversePaginatedResource[Location](uri, auth, dateFrom, dateTo)

  def locationsSource(uri: Uri, auth: AuthToken[Ours], dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Source[Location, NotUsed] =
    PaginatedSource[Location](http, uri, auth, dateFrom, dateTo)

}
