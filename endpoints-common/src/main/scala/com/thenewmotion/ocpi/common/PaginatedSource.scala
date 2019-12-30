package com.thenewmotion.ocpi
package common

import java.time.ZonedDateTime
import _root_.akka.NotUsed
import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.client.RequestBuilding.Get
import _root_.akka.http.scaladsl.model.Uri.Query
import _root_.akka.http.scaladsl.model.{HttpRequest, Uri}
import _root_.akka.http.scaladsl.unmarshalling.Unmarshal
import _root_.akka.stream.Materializer
import _root_.akka.stream.scaladsl.Source
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.{AuthToken, ErrorResp}
import com.thenewmotion.ocpi.ZonedDateTimeParser._
import cats.syntax.either._
import cats.instances.future._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class PaginationException(uri: Uri, cause: Throwable)
  extends RuntimeException(s"An error occurred when calling ${uri.toString}", cause)

object PaginatedSource extends AuthorizedRequests with EitherUnmarshalling with OcpiResponseUnmarshalling {

  private def singleRequestWithNextLink[T](
    http: HttpExt,
    req: HttpRequest,
    auth: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    successU: PagedRespUnMar[T],
    errorU: ErrRespUnMar
  ): Future[Either[ErrorResp, (PagedResp[T], Option[Uri])]] =
    (for {
      response <- result(requestWithAuth(http, req, auth).map(_.asRight))
      success <- result(Unmarshal(response).to[ErrorRespOr[(PagedResp[T], Option[Uri])]])
    } yield success).value.recoverWith {
      case NonFatal(ex) => Future.failed(PaginationException(req.uri, ex))
    }

  def apply[T](
    http: HttpExt,
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    limit: Int = 100
  )(implicit ec: ExecutionContext, mat: Materializer,
    successU: PagedRespUnMar[T], errorU: ErrRespUnMar): Source[T, NotUsed] = {
    val query = Query(Map(
      "offset" -> "0",
      "limit" -> limit.toString) ++
      dateFrom.map("date_from" -> format(_)) ++
      dateTo.map("date_to" -> format(_))
    )

    Source.unfoldAsync[Option[Uri], Iterable[T]](Some(uri withQuery query)) {
      case Some(x) =>
        singleRequestWithNextLink[T](http, Get(x), auth).map {
          case Left(err) => throw OcpiClientException(err)
          case Right((success, u)) => Some((u, success.data))
        }
      case None => Future.successful(None)
    }.mapConcat(_.toList)
  }
}
