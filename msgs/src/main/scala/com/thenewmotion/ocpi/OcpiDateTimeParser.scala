package com.thenewmotion.ocpi

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import DateTimeFormatter._

import scala.util.Try

object OcpiDateTimeParser {

  private val formatter =
    new DateTimeFormatterBuilder()
      .append(ISO_LOCAL_DATE_TIME)
      .optionalStart
      .appendLiteral('Z')
      .optionalEnd
      .toFormatter
      .withZone(ZoneOffset.UTC)

  def format(dt: ZonedDateTime): String = dt.format(formatter)

  def parseOpt(dt: String): Option[ZonedDateTime] =
    Try(ZonedDateTime.parse(dt, formatter)).toOption

  def parse(dt: String): ZonedDateTime =
    parseOpt(dt).getOrElse(
      throw new IllegalArgumentException("Expected DateTime conforming to pattern " +
        "specified in OCPI 21.2 section 14.2, but got " + dt)
    )

}
