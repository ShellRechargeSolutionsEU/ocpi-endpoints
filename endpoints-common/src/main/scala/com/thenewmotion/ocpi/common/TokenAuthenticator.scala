package com.thenewmotion.ocpi
package common

import _root_.akka.http.scaladsl.model.headers.GenericHttpCredentials
import _root_.akka.http.scaladsl.model.headers.HttpChallenge
import _root_.akka.http.scaladsl.model.headers.HttpCredentials
import _root_.akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import msgs.Ownership.Theirs
import msgs.{AuthToken, GlobalPartyId}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TokenAuthenticator(toApiUser: AuthToken[Theirs] => Future[Option[GlobalPartyId]])(implicit executionContext: ExecutionContext)
  extends (Option[HttpCredentials] ⇒ Future[AuthenticationResult[GlobalPartyId]]) {

  lazy val challenge = HttpChallenge(scheme = "Token", realm = "ocpi")

  override def apply(credentials: Option[HttpCredentials]): Future[AuthenticationResult[GlobalPartyId]] = {
    credentials
      .flatMap {
        case GenericHttpCredentials("Token", _, params) => params.headOption.map(_._2)
        case _ => None
      } match {
        case None => Future.successful(Left(challenge))
        case Some(x) => toApiUser(AuthToken[Theirs](x)).map {
          case Some(x2) => Right(x2)
          case None => Left(challenge)
        }
      }
  }
}
