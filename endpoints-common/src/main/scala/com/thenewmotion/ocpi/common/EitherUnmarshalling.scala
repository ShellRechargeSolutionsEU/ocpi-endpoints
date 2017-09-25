package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller.EitherUnmarshallingException
import akka.http.scaladsl.unmarshalling._
import akka.util.ByteString

import scala.concurrent.Future
import scala.reflect.ClassTag

trait EitherUnmarshalling {

  implicit def eitherUnmarshaller[L, R](
    implicit ua: FromByteStringUnmarshaller[L], rightTag: ClassTag[R],
             ub: FromByteStringUnmarshaller[R], leftTag: ClassTag[L]): FromEntityUnmarshaller[Either[L, R]] =

    Unmarshaller.withMaterializer[HttpEntity, Either[L, R]] { implicit ex ⇒ implicit mat ⇒ value ⇒
      import akka.http.scaladsl.util.FastFuture._

      @inline def right(s: ByteString) = ub(s).fast.map(Right(_))

      @inline def fallbackLeft(s: ByteString): PartialFunction[Throwable, Future[Either[L, R]]] = { case rightFirstEx ⇒
        val left = ua(s).fast.map(Left(_))

        left.transform(
          s = x => x,
          f = leftSecondEx => EitherUnmarshallingException(
            rightClass = rightTag.runtimeClass, right = rightFirstEx,
            leftClass = leftTag.runtimeClass, left = leftSecondEx)
        )
      }

      Unmarshal(value).to[ByteString].flatMap { s =>
        right(s).recoverWith(fallbackLeft(s))
      }
    }

}
