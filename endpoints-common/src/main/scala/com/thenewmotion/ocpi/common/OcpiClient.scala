package com.thenewmotion.ocpi
package common

import java.time.ZonedDateTime

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.scaladsl.Sink
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.{AuthToken, ErrorResp, SuccessResp}
import cats.syntax.either._

import scala.util.control.NonFatal

//cf. chapter 3.1.3 from the OCPI 2.1 spec
class ClientObjectUri (val value: Uri) extends AnyVal

object ClientObjectUri {
  def apply(
    endpointUri: Uri,
    ourCountryCode: String,
    ourPartyId: String,
    ids: String*
  ): ClientObjectUri = {
    val epUriNormalised =
      if (endpointUri.path.endsWithSlash) endpointUri.path
      else endpointUri.path ++ Uri.Path./

    val rest = ids.foldLeft(Uri.Path(ourCountryCode) / ourPartyId)(_ / _)
    new ClientObjectUri(endpointUri.withPath(epUriNormalised ++ rest))
  }
}

/**
 * Internally used to carry failure information through Akka Streams
 */
private[common] case class OcpiClientException(errorResp: ErrorResp) extends Exception

/**
 * Thrown to signal an error where no valid OCPI response was produced by the server
 */
case class FailedRequestException(request: HttpRequest, response: HttpResponse, cause: Throwable)
  extends Exception("Failed to get response to OCPI request", cause)

abstract class OcpiClient(MaxNumItems: Int = 100)(implicit http: HttpExt)
  extends AuthorizedRequests with EitherUnmarshalling with OcpiResponseUnmarshalling {

  def singleRequest[T](
    req: HttpRequest,
    auth: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    errorU: ErrRespUnMar,
    sucU: SuccessRespUnMar[T]
  ): Future[ErrorRespOr[SuccessResp[T]]] =
    requestWithAuth(http, req, auth).flatMap { response =>
      Unmarshal(response).to[ErrorRespOr[SuccessResp[T]]].recover {
        case NonFatal(cause) => throw FailedRequestException(req, response, cause)
      }
    }

  def traversePaginatedResource[T](
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    limit: Int = MaxNumItems
  )(
    implicit ec: ExecutionContext, mat: Materializer, successU: PagedRespUnMar[T], errorU: ErrRespUnMar
  ): Future[ErrorRespOr[Iterable[T]]] =
    PaginatedSource[T](http, uri, auth, dateFrom, dateTo, limit).runWith(Sink.seq[T]).map {
      _.asRight
    }.recover {
      case OcpiClientException(errorResp) => errorResp.asLeft
    }
}
