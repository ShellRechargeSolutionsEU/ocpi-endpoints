package com.thenewmotion.ocpi.common

import spray.httpx.marshalling._
import scalaz.\/


trait ResponseMarshalling {
  implicit def eitherToResponseMarshaller[L, R]
    (implicit lm: ToResponseMarshaller[L], rm: ToResponseMarshaller[R]) =
    ToResponseMarshaller[\/[L, R]]{ (v, ctx) =>
      v.fold(lm(_, ctx), rm(_, ctx))
    }
}
