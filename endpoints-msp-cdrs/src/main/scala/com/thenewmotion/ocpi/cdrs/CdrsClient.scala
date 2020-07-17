package com.thenewmotion.ocpi
package cdrs

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
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.Cdr
import scala.concurrent.ExecutionContext

class CdrsClient(
  implicit http: HttpExt,
  successU: PagedRespUnMar[Cdr],
  errorU: ErrRespUnMar
) extends OcpiClient {

  def getCdrs(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Iterable[Cdr]]] =
    traversePaginatedResource[Cdr](uri, auth, dateFrom, dateTo, pageLimit)

  def cdrsSource(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Source[Cdr, NotUsed] =
    PaginatedSource[Cdr](http, uri, auth, dateFrom, dateTo, pageLimit)

}
