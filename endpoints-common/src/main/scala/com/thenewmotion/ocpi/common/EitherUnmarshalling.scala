package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller.EitherUnmarshallingException
import akka.http.scaladsl.unmarshalling._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait EitherUnmarshalling {

  implicit def eitherUnmarshaller[L, R](
    implicit ua: FromEntityUnmarshaller[L], rightTag: ClassTag[R],
             ub: FromEntityUnmarshaller[R], leftTag: ClassTag[L]): FromEntityUnmarshaller[Either[L, R]] =

    Unmarshaller.withMaterializer[HttpEntity, Either[L, R]] { implicit ex ⇒ implicit mat ⇒ value ⇒
      import akka.http.scaladsl.util.FastFuture._

      @inline def right(e: HttpEntity) = ub(e).fast.map(Right(_))

      @inline def fallbackLeft(e: HttpEntity): PartialFunction[Throwable, Future[Either[L, R]]] = { case rightFirstEx ⇒
        val left = ua(e).fast.map(Left(_))

        left.transform(
          s = x => x,
          f = leftSecondEx => EitherUnmarshallingException(
            rightClass = rightTag.runtimeClass, right = rightFirstEx,
            leftClass = leftTag.runtimeClass, left = leftSecondEx)
        )
      }

      for {
        e <- value.httpEntity.toStrict(10.seconds)
        res <- right(e).recoverWith(fallbackLeft(e))
      } yield res
    }

}
