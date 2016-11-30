package com.thenewmotion.ocpi.common

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.common.ClientError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{OcpiEnvelope, Page}
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode
import spray.client.pipelining._
import spray.http.HttpHeaders.Link
import spray.http._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/, \/-}


abstract class OcpiClient(val MaxNumItems: Int = 100)(implicit refFactory: ActorRefFactory, requestTimeout: Timeout) {

  protected val logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }


  protected[ocpi] def setPageLimit(linkUri: Uri) = {
    val newLimit = linkUri.query.get("limit").map(_.toInt min MaxNumItems) getOrElse MaxNumItems
    linkUri.withQuery(linkUri.query.toMap + ("limit" -> newLimit.toString))
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  def sendAndReceive = sendReceive

  protected def request(auth: String)(implicit ec: ExecutionContext) = (
    addCredentials(GenericHttpCredentials("Token", auth, Map()))
      ~> logRequest
      ~> sendAndReceive
      ~> logResponse
    )

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
    (dataUnmarshaller: HttpResponse => Page[R])
    (implicit ec: ExecutionContext): FTS[ClientError, R] = {
      val fullParams: Map[String, String] =
        Map(
          "offset" -> "0",
          "limit" -> limit.toString) ++ queryParams
      _traversePaginatedResource(uri withQuery fullParams, auth)(dataUnmarshaller)
    }

  private def exception2ClientError: PartialFunction[Throwable,-\/[ClientError]] = {
    case ex: spray.httpx.PipelineException =>
      -\/(UnmarshallingFailed(Some(ex.getLocalizedMessage)))
    case ex: spray.can.Http.ConnectionAttemptFailedException =>
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
    (dataUnmarshaller: HttpResponse => Page[R])
    (implicit ec: ExecutionContext): FTS[ClientError, R] = {
    val pipeline = request(auth)

    withOcpiErrorHandler(pipeline(Get(uri))) { response: HttpResponse =>
      val accResp: Option[FTS[ClientError, R]] =
        response
          .header[Link]
          .flatMap(_.values.find(_.params.contains(Link.next)).map(_.uri))
          .map { nextUri =>
            logger.debug(s"following Link: $nextUri")
            _traversePaginatedResource(setPageLimit(nextUri), auth)(dataUnmarshaller)
          }
      val entity: Page[R] = response ~> dataUnmarshaller
      val accLocs = accResp.map {
        _.map { disj => \/-(entity.items ++ disj.getOrElse(Iterable.empty)) }
      } orElse Some(Future.successful(\/-(entity.items)))
      accLocs getOrElse Future.successful(\/-(Nil))
    }
  }

  protected def withOcpiErrorHandler[R]( resp: => Future[HttpResponse])(f: HttpResponse => FTS[ClientError, R]):
    FTS[ClientError, R] = {
    Try {
      resp.flatMap { response =>
        if (response.status.isSuccess) {
          val envelope = response ~> unmarshal[OcpiEnvelope]
          if(envelope.statusCode.isSuccess){
            f(response)
          } else Future.successful(-\/(ocpiEnvelope2ClientError(envelope)))
        } else Future.successful(-\/(httpStatusCode2ClientError(response.status)))
      } recover { exception2ClientError }
    } match {
      case Success(s) => s
      case Failure(e) => Future.successful(exception2ClientError(e))
    }
  }
}
