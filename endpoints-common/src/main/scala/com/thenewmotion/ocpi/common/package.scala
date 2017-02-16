package com.thenewmotion.ocpi

import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import common.PaginatedSource.PagedResp
import msgs.ErrorResp
import scala.concurrent.Future
import scalaz.{EitherT, \/}

package object common {
  type ErrUnMar = FromEntityUnmarshaller[ErrorResp]
  type SucUnMar[T] = FromEntityUnmarshaller[PagedResp[T]]

  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)
}
