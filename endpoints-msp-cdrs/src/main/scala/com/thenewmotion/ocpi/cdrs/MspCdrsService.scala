package com.thenewmotion.ocpi
package cdrs

import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.{Cdr, CdrId}
import msgs.GlobalPartyId


/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspCdrsService[F[_]] {

  /**
    * @return Either#Right if the cdr has been created
    */
  def createCdr(
    globalPartyId: GlobalPartyId,
    cdr: Cdr
  ): F[Either[CdrsError, Unit]]

  /**
    * @return existing Cdr or Error if Cdr couldn't be found
    */
  def cdr(
    globalPartyId: GlobalPartyId,
    cdrId: CdrId
  ): F[Either[CdrsError, Cdr]]
}
