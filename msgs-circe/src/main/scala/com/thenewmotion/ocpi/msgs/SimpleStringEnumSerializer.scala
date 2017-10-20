package com.thenewmotion.ocpi.msgs

import com.thenewmotion.ocpi.{Enumerable, Nameable}
import io.circe.{Decoder, Encoder}

object SimpleStringEnumSerializer {
  def encoder[T <: Nameable](enum: Enumerable[T]): Encoder[T] =
    Encoder.encodeString.contramap[T](_.name)

  def decoder[T <: Nameable](enum: Enumerable[T]): Decoder[T] =
    Decoder.decodeString.emap { x =>
      enum.withName(x).toRight(s"Unknown value: $x")
    }
}
