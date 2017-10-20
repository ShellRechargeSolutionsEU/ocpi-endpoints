package com.thenewmotion.ocpi
package common

import java.time.ZonedDateTime

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.{AuthToken, ErrorResp}
import com.thenewmotion.ocpi.ZonedDateTimeParser._
import cats.syntax.either._
import cats.instances.future._

import scala.concurrent.{ExecutionContext, Future}

object PaginatedSource extends AuthorizedRequests with EitherUnmarshalling with OcpiResponseUnmarshalling {

  private def singleRequestWithNextLink[T](
    http: HttpExt,
    req: HttpRequest,
    auth: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    successU: SucUnMar[T],
    errorU: ErrUnMar
  ): Future[Either[ErrorResp, (PagedResp[T], Option[Uri])]] =
    (for {
      response <- result(requestWithAuth(http, req, auth).map(_.asRight))
      success <- result(Unmarshal(response).to[Either[ErrorResp, (PagedResp[T], Option[Uri])]])
    } yield success).value

  def apply[T](
    http: HttpExt,
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    limit: Int = 100
  )(implicit ec: ExecutionContext, mat: Materializer,
      successU: SucUnMar[T], errorU: ErrUnMar): Source[T, NotUsed] = {
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
