package com.thenewmotion.ocpi

import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.data.EitherT
import common.PaginatedSource.PagedResp
import msgs.{ErrorResp, SuccessResp}

import scala.concurrent.Future

package object common {
  type ErrRespUnMar = FromEntityUnmarshaller[ErrorResp]
  type ErrRespMar = ToEntityMarshaller[ErrorResp]
  type SuccessRespUnMar[T] = FromEntityUnmarshaller[SuccessResp[T]]
  type SuccessRespMar[T] = ToEntityMarshaller[SuccessResp[T]]
  type PagedRespUnMar[T] = FromEntityUnmarshaller[PagedResp[T]]

  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[Either[L, T]]): Result[L, T] = EitherT(future)
}
