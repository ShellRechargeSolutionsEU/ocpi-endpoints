package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.model.headers.{Link, LinkParams}
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromResponseUnmarshaller, Unmarshaller}
import msgs.{ErrorResp, SuccessResponse, SuccessWithDataResp}
import scala.reflect.ClassTag
import scalaz.\/

case class UnexpectedResponseException(status : akka.http.scaladsl.model.StatusCode) extends Exception

trait OcpiResponseUnmarshalling {
  protected implicit def fromOcpiResponseUnmarshaller[T <: SuccessResponse : FromEntityUnmarshaller : ClassTag](
    implicit disjUnMa: FromEntityUnmarshaller[ErrorResp \/ T]): FromResponseUnmarshaller[ErrorResp \/ T] =
    Unmarshaller.withMaterializer[HttpResponse, ErrorResp \/ T] {
      implicit ex => implicit mat => response: HttpResponse =>
        if (response.status.isSuccess)
          disjUnMa(response.entity)
        else {
          response.discardEntityBytes()
          throw UnexpectedResponseException(response.status)
    }
  }

  type PagedResp[T] = SuccessWithDataResp[Iterable[T]]

  protected implicit def fromPagedOcpiResponseUnmarshaller[T](
    implicit um: FromResponseUnmarshaller[ErrorResp \/ PagedResp[T]]):
     FromResponseUnmarshaller[ErrorResp \/ (PagedResp[T], Option[Uri])] =

    Unmarshaller.withMaterializer[HttpResponse, ErrorResp \/ (PagedResp[T], Option[Uri])] {
      implicit ex => implicit mat => response: HttpResponse =>
        um(response).map { _.map {
          (x: PagedResp[T]) =>
            (x, response
                .header[Link]
                .flatMap(_.values.find(_.params.contains(LinkParams.next)).map(_.uri)))
        }}
    }
}
