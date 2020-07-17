package com.thenewmotion.ocpi
package tokens

import cats.Applicative
import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import msgs.GlobalPartyId
import msgs.v2_1.Tokens._
import cats.syntax.either._
import cats.syntax.option._
import com.thenewmotion.ocpi.tokens.TokenError.IncorrectTokenId


/**
  * All methods are to be implemented with idempotency semantics.
  */
trait CpoTokensService[F[_]] {

  /**
    * @return retrieve the token if it exists, otherwise returns TokenNotFound Error
    */
  def token(globalPartyId: GlobalPartyId, tokenUid: TokenUid): F[Either[TokenError, Token]]

  protected[tokens] def createOrUpdateToken(
    apiUser: GlobalPartyId,
    tokenUid: TokenUid,
    token: Token
  )(
    implicit A: Applicative[F]
  ): F[Either[TokenError, CreateOrUpdateResult]] = {
    if (token.uid == tokenUid) {
      createOrUpdateToken(apiUser, token)
    } else
      Applicative[F].pure(
        IncorrectTokenId(s"Token id from Url is $tokenUid, but id in JSON body is ${token.uid}".some).asLeft
      )
  }

  /**
    * returns TokenCreationFailed if an error occurred.
    */
  def createOrUpdateToken(
    globalPartyId: GlobalPartyId,
    token: Token
  ): F[Either[TokenError, CreateOrUpdateResult]]

  /**
    * returns TokenUpdateFailed if an error occurred.
    */
  def updateToken(
    globalPartyId: GlobalPartyId,
    tokenUid: TokenUid,
    tokenPatch: TokenPatch
  ): F[Either[TokenError, Unit]]
}
