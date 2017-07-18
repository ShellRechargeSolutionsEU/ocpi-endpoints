package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller.EitherUnmarshallingException
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import cats.syntax.either._

trait EitherMarshalling {
  implicit def disjToResponseMarshaller[L, R]
  (implicit lm: ToResponseMarshaller[L], rm: ToResponseMarshaller[R]) =
    Marshaller {
      implicit ex: ExecutionContext =>
        value: Either[L, R] => value.fold(lm(_), rm(_))
    }

  implicit def disjFromEntityUnmarshaller[A, B](
    implicit ua: FromEntityUnmarshaller[A], rightTag: ClassTag[A],
      ub: FromEntityUnmarshaller[B], leftTag: ClassTag[B]): FromEntityUnmarshaller[Either[A, B]] =
    Unmarshaller.withMaterializer {
      implicit ex: ExecutionContext =>
        implicit mat: Materializer =>
          value: HttpEntity =>
            import akka.http.scaladsl.util.FastFuture._
            // unmarshal as B and if successful pack it as \/-
            def right = ub(value).fast.map(_.asRight)
            // since A is our "2nd try" we want to keep the first exception here too!
            def fallbackLeft: PartialFunction[Throwable, Future[Either[A, B]]] = {
              case rightFirstEx =>
                val left = ua(value).fast.map(_.asLeft)

                // combine EitherUnmarshallingException by carrying both exceptions
                left.transform(
                  s = x => x,
                  f = leftSecondEx => EitherUnmarshallingException(
                    rightClass = rightTag.runtimeClass, right = rightFirstEx,
                    leftClass = leftTag.runtimeClass, left = leftSecondEx)
                )
            }

            right.recoverWith(fallbackLeft)
    }
}
