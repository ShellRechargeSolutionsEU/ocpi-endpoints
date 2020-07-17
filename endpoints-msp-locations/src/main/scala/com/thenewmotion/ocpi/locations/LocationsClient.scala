package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime
import _root_.akka.NotUsed
import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import _root_.akka.stream.scaladsl.Source
import cats.effect.{ContextShift, IO}
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient, PagedRespUnMar, PaginatedSource}
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import scala.concurrent.ExecutionContext

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
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Iterable[Location]]] =
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
