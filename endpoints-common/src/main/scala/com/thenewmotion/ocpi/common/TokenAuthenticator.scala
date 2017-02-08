package com.thenewmotion.ocpi.common

import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import com.thenewmotion.ocpi.ApiUser
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TokenAuthenticator(toApiUser: String => Future[Option[ApiUser]])(implicit executionContext: ExecutionContext)
  extends (Option[HttpCredentials] ⇒ Future[AuthenticationResult[ApiUser]]) {

  lazy val challenge = HttpChallenge(scheme = "Token", realm = "ocpi")

  override def apply(credentials: Option[HttpCredentials]): Future[AuthenticationResult[ApiUser]] = {
    credentials
      .flatMap {
        case GenericHttpCredentials("Token", _, params) => params.headOption.map(_._2)
        case _ => None
      } match {
        case None => Future.successful(Left(challenge))
        case Some(x) => toApiUser(x).map {
          case Some(x2) => Right(x2)
          case None => Left(challenge)
        }
      }
  }
}
