package com.thenewmotion.ocpi
package msgs

import org.joda.time.DateTimeZone
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import spray.json._

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

object OcpiDatetimeParser {

  def toOcpiDateTime(dt: String) = {
    import com.thenewmotion.time.JodaImplicits._

    DateTimeZone.setDefault(DateTimeZone.UTC)
    val (formatterNoMillis, formatterNoTz, formatterWithTz) =
      (ISODateTimeFormat.dateTimeNoMillis.withZoneUTC.parseOption(dt),
       DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").parseOption(dt),
       ISODateTimeFormat.dateTime.withZoneUTC.parseOption(dt))

    formatterNoMillis orElse formatterNoTz orElse formatterWithTz
  }

}