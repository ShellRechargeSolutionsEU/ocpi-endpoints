package com.thenewmotion.ocpi

import akka.http.scaladsl.unmarshalling.FromByteStringUnmarshaller
import cats.data.EitherT
import common.PaginatedSource.PagedResp
import msgs.ErrorResp

import scala.concurrent.Future

package object common {
  type ErrUnMar = FromByteStringUnmarshaller[ErrorResp]
  type SucUnMar[T] = FromByteStringUnmarshaller[PagedResp[T]]

  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[Either[L, T]]): Result[L, T] = EitherT(future)
}
