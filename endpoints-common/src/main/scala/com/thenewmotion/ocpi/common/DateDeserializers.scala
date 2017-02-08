package com.thenewmotion.ocpi.common

import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.thenewmotion.ocpi.msgs.OcpiDatetimeParser
import org.joda.time.DateTime

trait DateDeserializers {
  implicit val String2OcpiDate = Unmarshaller.strict[String, DateTime] { value =>
    OcpiDatetimeParser.toOcpiDateTime(value) match {
      case Some(x) => x
      case None => throw new IllegalArgumentException(s"$value is not a valid OCPI Datetime")
    }
  }
}
