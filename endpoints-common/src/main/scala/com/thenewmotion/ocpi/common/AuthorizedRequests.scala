package com.thenewmotion.ocpi.common

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.Logger
import scala.concurrent.{ExecutionContext, Future}

trait AuthorizedRequests {

  protected val logger = Logger(getClass)

  // setup request/response logging
  private val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  private val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  protected def requestWithAuth(http: HttpExt, req: HttpRequest, auth: String)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HttpResponse] = {
    http.singleRequest(logRequest(req.addCredentials(GenericHttpCredentials("Token", auth, Map())))).map { response =>
      logResponse(response)
      response
    }
  }
}
