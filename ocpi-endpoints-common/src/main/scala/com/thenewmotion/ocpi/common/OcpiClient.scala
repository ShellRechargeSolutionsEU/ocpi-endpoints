package com.thenewmotion.ocpi.common

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Page
import spray.client.pipelining._
import spray.http.HttpHeaders.Link
import spray.http._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/, \/-}


abstract class OcpiClient(implicit refFactory: ActorRefFactory, requestTimeout: Timeout) {

  protected val logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  val MaxNumItems = 100

  protected[ocpi] def setPageLimit(linkUri: Uri) = {
    val newLimit = linkUri.query.get("limit").map(_.toInt min MaxNumItems) getOrElse MaxNumItems
    linkUri.withQuery(linkUri.query.toMap + ("limit" -> newLimit.toString))
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def sendAndReceive = sendReceive

  protected def request(auth: String)(implicit ec: ExecutionContext) = (
    addCredentials(GenericHttpCredentials("Token", auth, Map()))
      ~> logRequest
      ~> sendAndReceive
      ~> logResponse
    )

  protected def bimap[T, M](f: Future[T])(pf: PartialFunction[Try[T], M])
    (implicit ec: ExecutionContext): Future[M] = {
    val p = Promise[M]()
    f.onComplete(r => p.complete(Try(pf(r))))
    p.future
  }

  type FTS[E, T] = Future[E \/ Iterable[T]]

  protected def traversePaginatedResource[E, R]
    (uri: Uri, auth: String, error: E, limit: Int = MaxNumItems)
    (page: HttpResponse => Page[R])
    (implicit ec: ExecutionContext): FTS[E, R] =
    _traversePaginatedResource(uri withQuery("offset" -> "0", "limit" -> limit.toString),
      auth, error)(page)

  private def _traversePaginatedResource[E, R](uri: Uri, auth: String, error: E)
    (respUnmarshaller: HttpResponse => Page[R])
    (implicit ec: ExecutionContext): FTS[E, R] = {
    val pipeline = request(auth)

    Try {
      pipeline(Get(uri))
      .flatMap { response =>
        val accResp: Option[FTS[E, R]] =
          response
          .header[Link]
          .flatMap(_.values.find(_.params.contains(Link.next)).map(_.uri))
          .map { nextUri =>
            logger.debug(s"following Link: $nextUri")
            _traversePaginatedResource(setPageLimit(nextUri), auth, error)(respUnmarshaller)
          }

        val entity: Page[R] = response ~> respUnmarshaller
        val accLocs = accResp.map {
          _.map { disj =>
            \/-(entity.items ++ (disj.getOrElse(Iterable.empty)))
          }
        } orElse Some(Future.successful(\/-(entity.items)))
        accLocs getOrElse Future.successful(-\/(error))
      }
    } match {
      case Success(s) => s
      case Failure(e) => Future.successful(-\/(error))
    }
  }
}
