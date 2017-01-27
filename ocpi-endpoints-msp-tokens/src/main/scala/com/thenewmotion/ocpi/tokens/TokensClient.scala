package com.thenewmotion.ocpi.tokens

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.common.{ClientObjectUri, OcpiClient}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{ErrorResp, SuccessResp, SuccessWithDataResp}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{Token, TokenPatch}
import scala.concurrent.{ExecutionContext, Future}
import scalaz._

class TokensClient(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends OcpiClient {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def getToken(tokenUri: ClientObjectUri, authToken: String, tokenUid: String)
    (implicit ec: ExecutionContext): Future[ErrorResp \/ Token] =
    singleRequest[SuccessWithDataResp[Token]](Get(tokenUri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve token from ${tokenUri.value}. Reason: $err")
        err
      }, _.data)
    }

  private def push(tokenUri: ClientObjectUri, authToken: String, rb: Uri => HttpRequest)
    (implicit ec: ExecutionContext): Future[ErrorResp \/ Unit] =
    singleRequest[SuccessResp](rb(tokenUri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not upload token to ${tokenUri.value}. Reason: $err")
        err
      }, _ => ())
    }

  def uploadToken(tokenUri: ClientObjectUri, authToken: String, token: Token)
    (implicit ec: ExecutionContext): Future[ErrorResp \/ Unit] =
    push(tokenUri, authToken, uri => Put(uri, token))


  def updateToken(tokenUri: ClientObjectUri, authToken: String, patch: TokenPatch)
    (implicit ec: ExecutionContext): Future[ErrorResp \/ Unit] =
    push(tokenUri, authToken, uri => Patch(uri, patch))

}

