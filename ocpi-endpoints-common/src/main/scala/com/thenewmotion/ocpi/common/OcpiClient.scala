package com.thenewmotion.ocpi.common

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, Link, LinkParams}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.client.RequestBuilding._
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.common.ClientError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{OcpiEnvelope, Page}
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode
import spray.json.JsonParser
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.Try
import scalaz.{-\/, \/, \/-}

abstract class OcpiClient(MaxNumItems: Int = 100)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

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

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  protected def requestWithAuth(req: HttpRequest, auth: String)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    http.singleRequest(logRequest(req.addCredentials(GenericHttpCredentials("Token", auth, Map())))).map { response =>
      logResponse(response)
      response
    }
  }

  protected def singleRequest[T : FromEntityUnmarshaller](req: HttpRequest, auth: String)(implicit ec: ExecutionContext): Future[T] = {
    requestWithAuth(req, auth).flatMap { response =>
      logResponse(response)
      if (response.status.isSuccess) Unmarshal(response.entity).to[T]
      else {
        response.discardEntityBytes()
        Future.failed(new RuntimeException(s"Request failed with status ${response.status}"))
      }
    }
  }

  //FIXME: implement TNM-3524 to make this code reuse the OcpiErrorHandler from TNM-3460
  protected def bimap[T, M](f: Future[T])(pf: PartialFunction[Try[T], M])
    (implicit ec: ExecutionContext): Future[M] = {
    val p = Promise[M]()
    f.onComplete(r => p.complete(Try(pf(r))))
    p.future
  }

  type FTS[E, T] = Future[E \/ Iterable[T]]

  protected def traversePaginatedResource[R]
    (uri: Uri, auth: String, queryParams: Map[String, String] = Map.empty, limit: Int = MaxNumItems)
    (dataUnmarshaller: HttpResponse => Future[Page[R]])
    (implicit ec: ExecutionContext): FTS[ClientError, R] = {
      val fullParams = Query(Map(
          "offset" -> "0",
          "limit" -> limit.toString) ++ queryParams)
      _traversePaginatedResource(uri withQuery fullParams, auth)(dataUnmarshaller)
    }

  private def exception2ClientError: PartialFunction[Throwable,-\/[ClientError]] = {
    case ex: JsonParser.ParsingException =>
      -\/(UnmarshallingFailed(Some(ex.getLocalizedMessage)))
    case ex: java.io.IOException =>
      -\/(ConnectionFailed(Some(ex.getLocalizedMessage)))
    case NonFatal(ex) => -\/(Unknown(Some(ex.toString)))
  }

  private def httpStatusCode2ClientError(statCode: StatusCode): ClientError = statCode match {
    case StatusCodes.NotFound => NotFound()
    case x => Unknown(Some(x.defaultMessage))
  }

  private def ocpiEnvelope2ClientError(ocpiEnvl: OcpiEnvelope): ClientError = {
    import OcpiStatusCode._
    ocpiEnvl.statusCode match {
      case GenericClientFailure       => OcpiClientError(ocpiEnvl.statusMessage)
      case InvalidOrMissingParameters => MissingParameters(ocpiEnvl.statusMessage)
      case GenericServerFailure       => OcpiServerError(ocpiEnvl.statusMessage)

      case x => Unknown(Some(Seq(Some(s"OCPI error. Code: ${x.code}"), ocpiEnvl.statusMessage).flatten.mkString(". Msg: ")))
    }
  }

  private def _traversePaginatedResource[R](uri: Uri, auth: String)
    (dataUnmarshaller: HttpResponse => Future[Page[R]])
    (implicit ec: ExecutionContext): FTS[ClientError, R] =
    withOcpiErrorHandler(requestWithAuth(Get(uri), auth)) { response: HttpResponse =>
      val accResp: Option[FTS[ClientError, R]] =
        response
          .header[Link]
          .flatMap(_.values.find(_.params.contains(LinkParams.next)).map(_.uri))
          .map { nextUri =>
            logger.debug(s"following Link: $nextUri")
            _traversePaginatedResource(setPageLimit(nextUri), auth)(dataUnmarshaller)
          }
      (response ~> dataUnmarshaller).flatMap { entity =>
        val accLocs = accResp.map {
          _.map { disj => \/-(entity.items ++ disj.getOrElse(Iterable.empty)) }
        } orElse Some(Future.successful(\/-(entity.items)))
        accLocs getOrElse Future.successful(\/-(Nil))
      }
    }

  protected def withOcpiErrorHandler[R](resp: => Future[HttpResponse])
    (f: HttpResponse => FTS[ClientError, R])
    (implicit ec: ExecutionContext): FTS[ClientError, R] = {
    resp.flatMap { response =>
      if (response.status.isSuccess) {
        for {
          envelope <- Unmarshal(response.entity).to[OcpiEnvelope]
          result   <- if (envelope.statusCode.isSuccess) f(response)
                      else Future.successful(-\/(ocpiEnvelope2ClientError(envelope)))
        } yield result
      } else {
        response.discardEntityBytes()
        Future.successful(-\/(httpStatusCode2ClientError(response.status)))
      }
    } recover exception2ClientError
  }
}
