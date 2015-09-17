package com.thenewmotion.ocpi

import spray.json._

package object msgs {
  trait Nameable {
    val name: String
  }

  trait Enumerable[T <: Nameable] {
    def values: Seq[T]
    def withName(name: String): Option[T] = values.find(_.name equals name)
  }

  class SimpleStringEnumSerializer[T <: Nameable](enum: Enumerable[T]) {
    implicit val enumFormat = new JsonFormat[T] {
      def write(x: T) = JsString(x.name)
      def read(value: JsValue) = value match {
        case JsString(x) => enum.withName(x).getOrElse(serializationError(s"Unknown " +
          s"value: $x"))
        case x => deserializationError("Expected value as JsString, but got " + x)
      }
    }
  }
}
