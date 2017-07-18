package com.thenewmotion.ocpi.tokens

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, Token}
import scala.concurrent.{ExecutionContext, Future}

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
    pager: Pager,
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  )(implicit ec: ExecutionContext): Future[PaginatedResult[Token]]

  def authorize(tokenUid: String, locationReferences: Option[LocationReferences])
    (implicit ec: ExecutionContext): Future[Either[AuthorizeError, AuthorizationInfo]]
}
