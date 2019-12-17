package com.thenewmotion.ocpi.cdrs

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.Cdr

import scala.concurrent.Future

trait CpoCdrsService {
  def cdrs(globalPartyId: GlobalPartyId,
           pager: Pager,
           dateFrom: Option[ZonedDateTime] = None,
           dateTo: Option[ZonedDateTime] = None
          ): Future[Either[CdrError, PaginatedResult[Cdr]]]
}
