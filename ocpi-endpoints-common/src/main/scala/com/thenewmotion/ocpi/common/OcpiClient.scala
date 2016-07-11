package com.thenewmotion.ocpi.common

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{DataResponse, SuccessResponse}
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
  val UrlRegex = """https?://[^\s/$.?#].[^\s]*""".r
  val LinkHeaderRegex = s"""<($UrlRegex)>(?:\\s*;\\s+rel="?([a-z]+)"?)""".r

  protected[ocpi] def extractNextUri(linkHeaderValue: String): Option[Uri] = {
    val m = LinkHeaderRegex.findAllIn(linkHeaderValue).matchData.toList

    if (m.isEmpty) throw UnknownLinkFormat(s"Couldn't parse Link value with regex $LinkHeaderRegex")

    m.find(_.group(2) == "next").map(next => Uri(next.group(1)))
  }

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

  protected def traversePaginatedResource[E, R <: SuccessResponse with DataResponse[R]]
    (uri: Uri, auth: String, error: E, limit: Int = MaxNumItems)
    (responseTransformation: HttpResponse => R)
    (implicit ec: ExecutionContext): Future[E \/ R] =
    _traversePaginatedResource(uri withQuery("offset" -> "0", "limit" -> limit.toString),
      auth, error)(responseTransformation)

  private def _traversePaginatedResource[E, R <: SuccessResponse with DataResponse[R]](uri: Uri, auth: String, error: E)
    (respUnmarshaller: HttpResponse => R)
    (implicit ec: ExecutionContext): Future[E \/ R] = {
    val pipeline = request(auth)
    lazy val resp = pipeline(Get(uri))

    Try {
      resp.flatMap { r =>
        val accResp: Option[Future[\/[E, R]]] = r.headers.find(_.name == Link.name) flatMap {
          linkHeader =>
            extractNextUri(linkHeader.value).map{ nextUri =>
              logger.debug(s"following Link: $nextUri")
              _traversePaginatedResource(setPageLimit(nextUri), auth, error)(respUnmarshaller)
            }
        }
        val rsp: R = r ~> respUnmarshaller
        val accLocs = accResp.map {
          _.map { disj =>
            val d = rsp.data ++ (disj.map(_.data) getOrElse Nil)
            \/-(rsp.copyData(data = d.asInstanceOf[List[rsp.DataItem]] ))
          }
        } orElse Some(Future.successful(\/-(rsp)))
        accLocs getOrElse Future.successful(-\/(error))
      }
    } match {
      case Success(s) => s
      case Failure(e) => Future.successful(-\/(error))
    }
  }
}



  case class UnknownLinkFormat(msg: String) extends Exception(msg)
  case class NoNextRelationFound(msg: String = "Couldn't find rel value 'next'") extends Exception(msg)
