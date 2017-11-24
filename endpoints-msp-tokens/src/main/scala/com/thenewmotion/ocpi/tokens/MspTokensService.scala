package com.thenewmotion.ocpi.tokens

import java.time.ZonedDateTime
import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, Token, TokenUid}
import scala.concurrent.Future

sealed trait AuthorizeError

object AuthorizeError {
  case object MustProvideLocationReferences extends AuthorizeError
  case object TokenNotFound extends AuthorizeError
}

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspTokensService {
  def tokens(
    globalPartyId: GlobalPartyId,
    pager: Pager,
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  ): Future[PaginatedResult[Token]]

  def authorize(
    globalPartyId: GlobalPartyId,
    tokenUid: TokenUid,
    locationReferences: Option[LocationReferences]
  ): Future[Either[AuthorizeError, AuthorizationInfo]]
}
