package com.thenewmotion.ocpi
package tokens

import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import msgs.GlobalPartyId
import msgs.v2_1.Tokens._
import cats.syntax.either._
import cats.syntax.option._
import com.thenewmotion.ocpi.tokens.TokenError.IncorrectTokenId

import scala.concurrent.Future

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait CpoTokensService {

  /**
    * @return retrieve the token if it exists, otherwise returns TokenNotFound Error
    */
  def token(globalPartyId: GlobalPartyId, tokenUid: TokenUid): Future[Either[TokenError, Token]]

  protected[tokens] def createOrUpdateToken(
    apiUser: GlobalPartyId,
    tokenUid: TokenUid,
    token: Token
  ): Future[Either[TokenError, CreateOrUpdateResult]] = {
    if (token.uid == tokenUid) {
      createOrUpdateToken(apiUser, token)
    } else
      Future.successful(
        IncorrectTokenId(s"Token id from Url is $tokenUid, but id in JSON body is ${token.uid}".some).asLeft
      )
  }

  /**
    * returns TokenCreationFailed if an error occurred.
    */
  def createOrUpdateToken(
    globalPartyId: GlobalPartyId,
    token: Token
  ): Future[Either[TokenError, CreateOrUpdateResult]]

  /**
    * returns TokenUpdateFailed if an error occurred.
    */
  def updateToken(
    globalPartyId: GlobalPartyId,
    tokenUid: TokenUid,
    tokenPatch: TokenPatch
  ): Future[Either[TokenError, Unit]]
}
