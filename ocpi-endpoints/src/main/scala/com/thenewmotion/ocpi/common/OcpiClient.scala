package com.thenewmotion.ocpi.common

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi._
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling._

import scala.concurrent.duration._
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.util.Try


abstract class OcpiClient(implicit refFactory: ActorRefFactory) {

  protected val logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  protected implicit val timeout = Timeout(10.seconds)

  protected def request(auth: String)(implicit ec: ExecutionContext) = (
    addCredentials(GenericHttpCredentials("Token", auth, Map()))
      ~> logRequest
      ~> sendReceive
      ~> logResponse
    )

  protected def unmarshalToOption[T](
    implicit unmarshaller: FromResponseUnmarshaller[T], ec: ExecutionContext
  ): Future[HttpResponse] => Future[Option[T]] = {
    _.map { res =>
      if (res.status.isFailure) None
      else Some(unmarshal[T](unmarshaller)(res))
    }
  }

  protected def bimap[T, M](f: Future[T])(pf: PartialFunction[Try[T], M])
    (implicit ec: ExecutionContext): Future[M] = {
    val p = Promise[M]()
    f.onComplete(r => p.complete(Try(pf(r))))
    p.future
  }
}
