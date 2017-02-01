package com.thenewmotion.ocpi.common

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.github.nscala_time.time.Imports.DateTime
import com.thenewmotion.ocpi.formatterNoMillis
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import scala.concurrent.{ExecutionContext, Future}
import scalaz._, Scalaz._

object PaginatedSource extends AuthorizedRequests with DisjunctionMarshalling with OcpiResponseUnmarshalling {
  private def singleRequestWithNextLink[T](http: HttpExt, req: HttpRequest, auth: String)
    (implicit ec: ExecutionContext, mat: ActorMaterializer, successU: SucUnMar[T],
     errorU: ErrUnMar): Future[\/[ErrorResp, (PagedResp[T], Option[Uri])]] =
    (for {
      response <- result(requestWithAuth(http, req, auth).map(\/-(_)))
      success <- result(Unmarshal(response).to[ErrorResp \/ (PagedResp[T], Option[Uri])])
    } yield success).run

  def apply[T](http: HttpExt, uri: Uri, auth: String, dateFrom: Option[DateTime] = None,
    dateTo: Option[DateTime] = None, limit: Int = 100)
    (implicit ec: ExecutionContext, mat: ActorMaterializer, successU: SucUnMar[T], errorU: ErrUnMar): Source[T, NotUsed] = {
    val query = Query(Map(
      "offset" -> "0",
      "limit" -> limit.toString) ++
      dateFrom.map("date_from" -> formatterNoMillis.print(_)) ++
      dateTo.map("date_to" -> formatterNoMillis.print(_))
    )

    Source.unfoldAsync[Option[Uri], Iterable[T]](Some(uri withQuery query)) {
      case Some(x) =>
        singleRequestWithNextLink[T](http, Get(x), auth).map {
          case -\/(err) => throw OcpiClientException(err)
          case \/-((success, u)) => Some((u, success.data))
        }
      case None => Future.successful(None)
    }.mapConcat(_.toList)
  }
}
