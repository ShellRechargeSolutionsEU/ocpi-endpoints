package com.thenewmotion.ocpi.msgs.sprayjson

import com.thenewmotion.ocpi.{Enumerable, Nameable}
import spray.json._

trait SimpleStringEnumSerializer {
  implicit def nameableFormat[T <: Nameable: Enumerable]: JsonFormat[T] = new JsonFormat[T] {
    def write(x: T) = JsString(x.name)
    def read(value: JsValue) = value match {
      case JsString(x) =>
        implicitly[Enumerable[T]]
          .withName(x)
          .getOrElse(
            serializationError(
              s"Unknown " +
                s"value: $x"
            )
          )
      case x => deserializationError("Expected value as JsString, but got " + x)
    }
  }
}

object SimpleStringEnumSerializer extends SimpleStringEnumSerializer
