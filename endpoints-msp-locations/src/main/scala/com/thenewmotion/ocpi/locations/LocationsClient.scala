package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import _root_.akka.NotUsed
import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.model.Uri
import common.{ErrRespUnMar, OcpiClient, PaginatedSource, PagedRespUnMar}
import msgs.v2_1.Locations.Location
import _root_.akka.stream.Materializer
import _root_.akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.AuthToken

class LocationsClient(
  implicit http: HttpExt,
  successU: PagedRespUnMar[Location],
  errorU: ErrRespUnMar
) extends OcpiClient {

  def getLocations(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[ErrorRespOr[Iterable[Location]]] =
    traversePaginatedResource[Location](uri, auth, dateFrom, dateTo, pageLimit)

  def locationsSource(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Source[Location, NotUsed] =
    PaginatedSource[Location](http, uri, auth, dateFrom, dateTo, pageLimit)

}
