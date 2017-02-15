package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.stream.ActorMaterializer
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scalaz.{-\/, \/, \/-}
import akka.stream.scaladsl.Sink
import com.github.nscala_time.time.Imports._
import msgs.{ErrorResp, SuccessResponse}

//cf. chapter 3.1.3 from the OCPI 2.1 spec
class ClientObjectUri (val value: Uri) extends AnyVal

object ClientObjectUri {
  def apply(endpointUri: Uri,
    ourCountryCode: String,
    ourPartyId: String,
    uid: String) = new ClientObjectUri(endpointUri.withPath(endpointUri.path / ourCountryCode / ourPartyId / uid))
}

case class OcpiClientException(errorResp: ErrorResp) extends Throwable

abstract class OcpiClient(MaxNumItems: Int = 100)(implicit http: HttpExt)
  extends AuthorizedRequests with DisjunctionMarshalling with OcpiResponseUnmarshalling {

  type ErrUnMar = FromEntityUnmarshaller[ErrorResp]
  type SucUnMar[T] = FromEntityUnmarshaller[PagedResp[T]]

  def singleRequest[T <: SuccessResponse : FromEntityUnmarshaller : ClassTag](req: HttpRequest, auth: String)
    (implicit ec: ExecutionContext, mat: ActorMaterializer, errorU: ErrUnMar): Future[ErrorResp \/ T] =
    requestWithAuth(http, req, auth).flatMap { response =>
      Unmarshal(response).to[ErrorResp \/ T]
    }

  def traversePaginatedResource[T]
    (uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None, limit: Int = MaxNumItems)
    (implicit ec: ExecutionContext, mat: ActorMaterializer, successU: SucUnMar[T], errorU: ErrUnMar): Future[ErrorResp \/ Iterable[T]] =
    PaginatedSource[T](http, uri, auth, dateFrom, dateTo, limit).runWith(Sink.seq[T]).map {
      \/-(_)
    }.recover {
      case OcpiClientException(errorResp) => -\/(errorResp)
    }
}
