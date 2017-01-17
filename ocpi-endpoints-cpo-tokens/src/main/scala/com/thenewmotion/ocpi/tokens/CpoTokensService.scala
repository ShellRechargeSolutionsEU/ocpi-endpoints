package com.thenewmotion.ocpi
package tokens

import com.thenewmotion.mobilityid.CountryCode
import com.thenewmotion.mobilityid.OperatorIdIso
import msgs.v2_1.Tokens._

import scala.concurrent.Future
import scalaz._

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait CpoTokensService {
  /**
    * @return retrieve the token if it exists, otherwise returns TokenNotFound Error
    */
  def token(countryCode: CountryCode, operatorId: OperatorIdIso, tokenUid: String): Future[TokenError \/ Token]

  /**
    * @return true if the token has been created and false if it has been updated.
    * returns TokenCreationFailed if an error occurred.
    */
  def createOrUpdateToken(countryCode: CountryCode, operatorId: OperatorIdIso, tokenUid: String, token: Token): Future[TokenError \/ Boolean]

  /**
    * returns TokenUpdateFailed if an error occurred.
    */
  def updateToken(countryCode: CountryCode, operatorId: OperatorIdIso, tokenUid: String, tokenPatch: TokenPatch): Future[TokenError \/ Unit]
}
