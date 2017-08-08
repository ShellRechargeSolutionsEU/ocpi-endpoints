package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.model.headers.{Link, LinkParams}
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromResponseUnmarshaller, Unmarshaller}
import msgs.{ErrorResp, SuccessResponse, SuccessWithDataResp}
import scala.reflect.ClassTag
import cats.syntax.either._

case class UnexpectedResponseException(response: HttpResponse)
  extends Exception(s"Unexpected HTTP status code ${response.status}")

trait OcpiResponseUnmarshalling {
  type ErrorRespOr[T] = Either[ErrorResp, T]

  protected implicit def fromOcpiResponseUnmarshaller[T <: SuccessResponse : FromEntityUnmarshaller : ClassTag](
    implicit disjUnMa: FromEntityUnmarshaller[ErrorRespOr[T]]
  ): FromResponseUnmarshaller[ErrorRespOr[T]] =
    Unmarshaller.withMaterializer[HttpResponse, ErrorRespOr[T]] {
      implicit ex => implicit mat => response: HttpResponse =>
        if (response.status.isSuccess)
          disjUnMa(response.entity)
        else {
          response.discardEntityBytes()
          throw UnexpectedResponseException(response)
    }
  }

  type PagedResp[T] = SuccessWithDataResp[Iterable[T]]

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
