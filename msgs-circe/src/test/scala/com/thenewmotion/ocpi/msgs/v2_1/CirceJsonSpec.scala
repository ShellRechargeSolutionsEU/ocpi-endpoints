package com.thenewmotion.ocpi.msgs.v2_1

import io.circe._
import io.circe.syntax._
import io.circe.parser

trait CirceJsonSpec extends GenericJsonSpec[Json, Decoder, Encoder] {

  override def parse(s: String): Json = parser.parse(s) match {
    case Left(ex: ParsingFailure) => throw ex
    case Right(x) => x
  }

  override def jsonStringToJson(s: String): Json = Json.fromString(s)

  override def jsonToObj[T : Decoder](j: Json): T =
    j.as[T] match {
      case Left(ex: DecodingFailure) => throw ex
      case Right(x) => x
    }

  override def objToJson[T : Encoder](t: T): Json = {
    // we can only remove the null keys when printing, so we print then re-parse, bit hacky but works
    val printer = Printer.noSpaces.copy(dropNullKeys = true)
    parse(printer.pretty(t.asJson))
  }
}
