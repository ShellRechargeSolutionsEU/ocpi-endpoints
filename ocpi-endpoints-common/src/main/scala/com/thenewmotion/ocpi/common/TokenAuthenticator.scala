package com.thenewmotion.ocpi.common

import akka.http.scaladsl.model.headers.{GenericHttpCredentials, HttpChallenge, HttpCredentials}
import akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import com.thenewmotion.ocpi.ApiUser

import scala.concurrent.Future

class TokenAuthenticator(
  apiUser: String => Option[ApiUser]
) extends (Option[HttpCredentials] ⇒ Future[AuthenticationResult[ApiUser]]) {
  override def apply(credentials: Option[HttpCredentials]): Future[AuthenticationResult[ApiUser]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future(
      credentials
        .flatMap {
          case GenericHttpCredentials("Token", token, _) => Some(token)
          case _ => None
        } flatMap apiUser match {
        case Some(x) => Right(x)
        case None => Left(HttpChallenge(scheme = "Token", realm = "ocpi"))
      }
    )
  }
}
