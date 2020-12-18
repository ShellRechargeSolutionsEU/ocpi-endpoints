package com.thenewmotion.ocpi.common

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, Location}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import com.thenewmotion.ocpi.Logger
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours

trait AuthorizedRequests {

  protected val logger: org.slf4j.Logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(HttpLogging.redactHttpRequest(r)); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(HttpLogging.redactHttpResponse(r)); r }

  private def requestWithAuthSupportingRedirect(
    http: HttpExt, req: HttpRequest, auth: AuthToken[Ours], redirectCount: Int = 0
  )(implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] = {
    http.singleRequest(
      logRequest(req.addCredentials(GenericHttpCredentials("Token", auth.value, Map())))
    ).flatMap { response =>
      logResponse(response)
      response.status match {
        case StatusCodes.PermanentRedirect | StatusCodes.TemporaryRedirect if redirectCount < 10 =>
          response.header[Location].map { newLoc =>
            logger.warn("Following redirect to {}", newLoc.uri)
            response.discardEntityBytes()
            requestWithAuthSupportingRedirect(http, req.withUri(newLoc.uri), auth, redirectCount + 1)
          }.getOrElse(Future.successful(response))
        case _ => Future.successful(response)
      }
    }
  }



  protected def requestWithAuth(http: HttpExt, req: HttpRequest, auth: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] =
    requestWithAuthSupportingRedirect(http, req, auth)
}
