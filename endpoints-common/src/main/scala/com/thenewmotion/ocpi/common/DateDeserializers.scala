package com.thenewmotion.ocpi.common

import java.time.ZonedDateTime

import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.thenewmotion.ocpi.ZonedDateTimeParser

trait DateDeserializers {
  implicit val String2OcpiDate = Unmarshaller.strict[String, ZonedDateTime] {
    value => ZonedDateTimeParser.parse(value)
  }
}
