package com.thenewmotion.ocpi.common

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.client.RequestBuilding._
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{ErrorResp, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scalaz.{EitherT, \/, \/-}
import scalaz.std.scalaFuture._

//cf. chapter 3.1.3 from the OCPI 2.1 spec
class ClientObjectUri (val value: Uri) extends AnyVal

object ClientObjectUri {
  def apply(endpointUri: Uri,
    ourCountryCode: String,
    ourPartyId: String,
    uid: String) = new ClientObjectUri(endpointUri.withPath(endpointUri.path / ourCountryCode / ourPartyId / uid))
}

abstract class OcpiClient(MaxNumItems: Int = 100)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
  extends DisjunctionMarshalling with OcpiResponseUnmarshalling {

  private val http = Http()

  protected val logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  protected[ocpi] def setPageLimit(linkUri: Uri) = {
    val newLimit = linkUri.query().get("limit").map(_.toInt min MaxNumItems) getOrElse MaxNumItems
    val newQuery = Query(linkUri.query().toMap + ("limit" -> newLimit.toString))
    linkUri withQuery newQuery
  }

  protected def requestWithAuth(req: HttpRequest, auth: String)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    http.singleRequest(logRequest(req.addCredentials(GenericHttpCredentials("Token", auth, Map())))).map { response =>
      logResponse(response)
      response
    }
  }

  def singleRequest[T <: SuccessResponse : FromEntityUnmarshaller : ClassTag](req: HttpRequest, auth: String)
    (implicit ec: ExecutionContext, eumErr: FromEntityUnmarshaller[ErrorResp]): Future[ErrorResp \/ T] =
    requestWithAuth(req, auth).flatMap { response =>
      Unmarshal(response).to[ErrorResp \/ T]
    }

  type FTS[T] = Future[ErrorResp \/ Iterable[T]]

  def traversePaginatedResource[T]
    (uri: Uri, auth: String, queryParams: Map[String, String] = Map.empty, limit: Int = MaxNumItems)
    (implicit ec: ExecutionContext,
     successUnmarshaller: FromEntityUnmarshaller[SuccessWithDataResp[Iterable[T]]],
     errorUnmarshaller: FromEntityUnmarshaller[ErrorResp]): FTS[T] = {
      val fullParams = Query(Map(
          "offset" -> "0",
          "limit" -> limit.toString) ++ queryParams)
      _traversePaginatedResource(uri withQuery fullParams, auth)
    }

  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  private def _traversePaginatedResource[T](uri: Uri, auth: String)
    (implicit ec: ExecutionContext,
     successUnmarshaller: FromEntityUnmarshaller[SuccessWithDataResp[Iterable[T]]],
     errorUnmarshaller: FromEntityUnmarshaller[ErrorResp]): FTS[T] = {

    def withNextPage(data: Iterable[T], nextUriOpt: Option[Uri]): Future[\/[ErrorResp, Iterable[T]]] =
      nextUriOpt.map { nextUri =>
        logger.debug(s"following Link: $nextUri")
        _traversePaginatedResource(setPageLimit(nextUri), auth)
      }.fold(Future.successful(\/-(data): ErrorResp \/ Iterable[T])) { n =>
        (for {
          next <- result(n)
        } yield {
          data ++ next
        }).run
      }

    (for {
      response <- result(requestWithAuth(Get(uri), auth).map(\/-(_)))
      success <- result(Unmarshal(response).to[ErrorResp \/ (PagedResp[T], Option[Uri])])
      (entity, nextUriOpt) = success
      x <- result(withNextPage(entity.data, nextUriOpt))
    } yield x).run
  }
}
