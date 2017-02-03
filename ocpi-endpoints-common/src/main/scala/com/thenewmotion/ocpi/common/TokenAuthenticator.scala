package com.thenewmotion.ocpi.common

import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import com.thenewmotion.ocpi.ApiUser

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TokenAuthenticator(toApiUser: String => Option[ApiUser])(
  implicit executionContext: ExecutionContext)
  extends (Option[HttpCredentials] ⇒ Future[AuthenticationResult[ApiUser]]) {
  override def apply(credentials: Option[HttpCredentials]): Future[AuthenticationResult[ApiUser]] = {
    Future(
      credentials
        .flatMap {
          case GenericHttpCredentials("Token", _, params) => params.headOption.map(_._2)
          case _ => None
        } flatMap toApiUser match {
        case Some(x) => Right(x)
        case None => Left(HttpChallenge(scheme = "Token", realm = "ocpi"))
      }
    )
  }
}
