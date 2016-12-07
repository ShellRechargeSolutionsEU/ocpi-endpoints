package com.thenewmotion.ocpi.common

import com.thenewmotion.ocpi.msgs.OcpiDatetimeParser
import org.joda.time.DateTime
import spray.httpx.unmarshalling.{Deserializer, MalformedContent}

trait DateDeserializers {
  implicit val String2OcpiDate = new Deserializer[String, DateTime] {
    def apply(value: String) =
      OcpiDatetimeParser.toOcpiDateTime(value) match {
        case Some(x) => Right(x)
        case None => Left(MalformedContent(s"$value is not a valid OCPI Datetime"))
      }

  }
}
