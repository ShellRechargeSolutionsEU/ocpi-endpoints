package com.thenewmotion.ocpi

import java.time.{LocalDate, LocalTime, ZoneOffset, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import DateTimeFormatter._
import java.time.temporal.ChronoField._

import scala.util.Try

object ZonedDateTimeParser {
  private val formatter =
    new DateTimeFormatterBuilder()
      .append(ISO_LOCAL_DATE_TIME)
      .optionalStart
      .appendLiteral('Z')
      .optionalEnd
      .toFormatter
      .withZone(ZoneOffset.UTC)

  def format(dt: ZonedDateTime): String = formatter.format(dt)

  def parseOpt(dt: String): Option[ZonedDateTime] =
    Try(ZonedDateTime.parse(dt, formatter)).toOption

  def parse(dt: String): ZonedDateTime =
    parseOpt(dt).getOrElse(
      throw new IllegalArgumentException("Expected DateTime conforming to pattern " +
        "specified in OCPI 21.2 section 14.2, but got " + dt)
    )

}

object LocalTimeParser {
  private val formatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendValue(HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(MINUTE_OF_HOUR, 2)
      .toFormatter

  def format(dt: LocalTime): String = formatter.format(dt)

  def parseOpt(dt: String): Option[LocalTime] =
    Try(LocalTime.parse(dt, formatter)).toOption

  def parse(dt: String): LocalTime =
    parseOpt(dt).getOrElse(
      throw new IllegalArgumentException(s"Expected LocalTime conforming to HH:mm, but got $dt")
    )
}

object LocalDateParser {
  val formatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendValue(YEAR, 4)
      .appendLiteral('-')
      .appendValue(MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(DAY_OF_MONTH, 2)
      .toFormatter


  def format(dt: LocalDate): String = formatter.format(dt)

  def parseOpt(dt: String): Option[LocalDate] =
    Try(LocalDate.parse(dt, formatter)).toOption

  def parse(dt: String): LocalDate =
    parseOpt(dt).getOrElse(
      throw new IllegalArgumentException(s"Expected LocalDate conforming to yyyy-MM-dd but got $dt")
    )
}
