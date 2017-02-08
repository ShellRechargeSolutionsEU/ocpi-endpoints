package com.thenewmotion.ocpi.tokens

import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, Token}
import org.joda.time.DateTime
import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

sealed trait AuthorizeError

object AuthorizeError {
  case object MustProvideLocationReferences extends AuthorizeError
  case object TokenNotFound extends AuthorizeError
}

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspTokensService {
  def tokens(pager: Pager, dateFrom: Option[DateTime] = None,
    dateTo: Option[DateTime] = None)(implicit ec: ExecutionContext): Future[PaginatedResult[Token]]

  def authorize(tokenUid: String, locationReferences: Option[LocationReferences])
    (implicit ec: ExecutionContext): Future[AuthorizeError \/ AuthorizationInfo]
}
