package com.thenewmotion.ocpi.common

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.thenewmotion.ocpi.Logger
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours

trait AuthorizedRequests {

  protected val logger: org.slf4j.Logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  protected def requestWithAuth(http: HttpExt, req: HttpRequest, auth: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] = {
    http.singleRequest(logRequest(req.addCredentials(GenericHttpCredentials("Token", auth.value, Map())))).map { response =>
      logResponse(response)
      response
    }
  }
}
