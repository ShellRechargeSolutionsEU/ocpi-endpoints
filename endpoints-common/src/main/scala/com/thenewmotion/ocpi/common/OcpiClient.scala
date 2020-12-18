package com.thenewmotion.ocpi
package common

import java.time.ZonedDateTime
import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.model.{DateTime => _, _}
import _root_.akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import _root_.akka.stream.Materializer
import scala.concurrent.{ExecutionContext, Future}
import _root_.akka.stream.scaladsl.Sink
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import msgs.{AuthToken, ErrorResp, SuccessResp}
import cats.syntax.either._
import scala.reflect.ClassTag
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
private[common] case class OcpiClientException(errorResp: ErrorResp)
  extends Exception(s"OCPI client failure with code ${errorResp.statusCode.code}: ${errorResp.statusMessage.getOrElse("no status message")}")

/**
 * Thrown to signal an error where no valid OCPI response was produced by the server
 */
case class FailedRequestException(request: HttpRequest, response: HttpResponse, cause: Throwable)
  extends Exception("Failed to get response to OCPI request", cause)

abstract class OcpiClient(implicit http: HttpExt)
  extends AuthorizedRequests with EitherUnmarshalling with OcpiResponseUnmarshalling {

  protected def singleRequestRawT[T : ClassTag](
    req: HttpRequest,
    auth: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    errorU: ErrRespUnMar,
    sucU: FromEntityUnmarshaller[T]
  ): Future[ErrorRespOr[T]] =
    requestWithAuth(http, req, auth).flatMap { response =>
      Unmarshal(response).to[ErrorRespOr[T]].recover {
        case NonFatal(cause) => throw FailedRequestException(req, response, cause)
      }
    }

  def singleRequest[T](
    req: HttpRequest,
    auth: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer,
    errorU: ErrRespUnMar,
    sucU: SuccessRespUnMar[T]
  ): Future[ErrorRespOr[SuccessResp[T]]] = singleRequestRawT[SuccessResp[T]](req, auth)

  def traversePaginatedResource[T](
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    limit: Int
  )(
    implicit ec: ExecutionContext, mat: Materializer, successU: PagedRespUnMar[T], errorU: ErrRespUnMar
  ): Future[ErrorRespOr[Iterable[T]]] =
    PaginatedSource[T](http, uri, auth, dateFrom, dateTo, limit).runWith(Sink.seq[T]).map {
      _.asRight
    }.recover {
      case OcpiClientException(errorResp) => errorResp.asLeft
    }
}

object OcpiClient {
  val DefaultPageLimit: Int = PaginatedSource.DefaultPageLimit
}