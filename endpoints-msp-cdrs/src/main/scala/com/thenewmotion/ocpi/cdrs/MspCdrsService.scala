package com.thenewmotion.ocpi
package cdrs

import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.Cdr
import msgs.GlobalPartyId

import scala.concurrent.Future

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspCdrsService {

  /**
    * @return Either#Right if the cdr has been created
    */
  def createCdr(
    globalPartyId: GlobalPartyId,
    cdr: Cdr
  ): Future[Either[CdrsError, Unit]]

  /**
    * @return existing Cdr or Error if Cdr couldn't be found
    */
  def cdr(
    globalPartyId: GlobalPartyId,
    cdrId: String
  ): Future[Either[CdrsError, Cdr]]
}
