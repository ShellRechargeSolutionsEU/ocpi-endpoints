package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.{ErrUnMar, OcpiClient, PaginatedSource, SucUnMar}
import msgs.v2_1.Locations.Location
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.AuthToken

class LocationsClient(
  implicit http: HttpExt,
  successU: SucUnMar[Location],
  errorU: ErrUnMar
) extends OcpiClient {

  def getLocations(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[ErrorRespOr[Iterable[Location]]] =
    traversePaginatedResource[Location](uri, auth, dateFrom, dateTo)

  def locationsSource(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Source[Location, NotUsed] =
    PaginatedSource[Location](http, uri, auth, dateFrom, dateTo)

}
