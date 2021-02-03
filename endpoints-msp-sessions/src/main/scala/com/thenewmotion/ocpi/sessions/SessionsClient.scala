package com.thenewmotion.ocpi.sessions

import java.time.ZonedDateTime
import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import cats.effect.{Async, ContextShift}
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient, PagedRespUnMar, PaginatedSource}
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.Session
import scala.concurrent.ExecutionContext

class SessionsClient[F[_]: Async](
  implicit http: HttpExt,
  successU: PagedRespUnMar[Session],
  errorU: ErrRespUnMar
) extends OcpiClient[F] {

  def getSessions(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: ZonedDateTime,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[F],
    mat: Materializer
  ): F[ErrorRespOr[Iterable[Session]]] =
    traversePaginatedResource[Session](uri, auth, Some(dateFrom), dateTo, pageLimit)

  def sessionsSource(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: ZonedDateTime,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Source[Session, NotUsed] =
    PaginatedSource[Session](http, uri, auth, Some(dateFrom), dateTo, pageLimit)

}
