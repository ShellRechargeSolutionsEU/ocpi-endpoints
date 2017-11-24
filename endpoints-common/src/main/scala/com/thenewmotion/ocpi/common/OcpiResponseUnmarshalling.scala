package com.thenewmotion.ocpi
package common

import _root_.akka.http.scaladsl.model.headers.{Link, LinkParams}
import _root_.akka.http.scaladsl.model.{HttpResponse, Uri}
import _root_.akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromResponseUnmarshaller, Unmarshaller}
import msgs.{ErrorResp, SuccessResp}
import cats.syntax.either._  // For Scala 2.11

case class UnexpectedResponseException(response: HttpResponse)
  extends Exception(s"Unexpected HTTP status code ${response.status}")

trait OcpiResponseUnmarshalling {
  type ErrorRespOr[T] = Either[ErrorResp, T]

  protected implicit def fromOcpiResponseUnmarshaller[D](
    implicit disjUnMa: FromEntityUnmarshaller[ErrorRespOr[D]]
  ): FromResponseUnmarshaller[ErrorRespOr[D]] =
    Unmarshaller.withMaterializer[HttpResponse, ErrorRespOr[D]] {
      implicit ex => implicit mat => response: HttpResponse =>
        if (response.status.isSuccess)
          disjUnMa(response.entity)
        else {
          response.discardEntityBytes()
          throw UnexpectedResponseException(response)
    }
  }

  type PagedResp[T] = SuccessResp[Iterable[T]]

  protected implicit def fromPagedOcpiResponseUnmarshaller[T](
    implicit um: FromResponseUnmarshaller[ErrorRespOr[PagedResp[T]]]
  ): FromResponseUnmarshaller[ErrorRespOr[(PagedResp[T], Option[Uri])]] =

    Unmarshaller.withMaterializer[HttpResponse, ErrorRespOr[(PagedResp[T], Option[Uri])]] {
      implicit ex => implicit mat => response: HttpResponse =>
        um(response).map { _.map {
          (x: PagedResp[T]) =>
            (x, response
                .header[Link]
                .flatMap(_.values.find(_.params.contains(LinkParams.next)).map(_.uri)))
        }}
    }
}
