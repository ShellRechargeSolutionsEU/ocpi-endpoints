package com.thenewmotion.ocpi.msgs.circe

import com.thenewmotion.ocpi.{Enumerable, Nameable}
import io.circe.{Decoder, Encoder}

trait SimpleStringEnumSerializer {
  implicit def encoder[T <: Nameable]: Encoder[T] =
    Encoder.encodeString.contramap[T](_.name)

  implicit def decoder[T <: Nameable: Enumerable]: Decoder[T] =
    Decoder.decodeString.emap { x =>
      implicitly[Enumerable[T]].withName(x).toRight(s"Unknown value: $x")
    }
}

object SimpleStringEnumSerializer extends SimpleStringEnumSerializer
