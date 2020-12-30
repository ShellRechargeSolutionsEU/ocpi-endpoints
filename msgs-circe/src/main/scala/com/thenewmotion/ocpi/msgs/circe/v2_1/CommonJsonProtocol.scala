package com.thenewmotion.ocpi.msgs
package circe.v2_1

import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}
import cats.syntax.either._
import OcpiStatusCode.SuccessCode
import v2_1.CommonTypes._
import com.thenewmotion.ocpi._
import io.circe.CursorOp.DownField
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}

import scala.reflect.{ClassTag, classTag}
import shapeless._

trait CommonJsonProtocol {

  implicit def nameableE[T <: Nameable]: Encoder[T] =
    Encoder.encodeString.contramap[T](_.name)

  implicit def nameableD[T <: Nameable: Enumerable]: Decoder[T] =
    Decoder.decodeString.emap { x =>
      implicitly[Enumerable[T]].withName(x).toRight(s"Unknown value: $x")
    }

  implicit def stringAnyValE[T](implicit g: Generic.Aux[T, String :: HNil]): Encoder[T] =
    stringEncoder(x => g.to(x).head)

  implicit def stringAnyValD[T](implicit g: Generic.Aux[T, String :: HNil]): Decoder[T] =
    tryStringDecoder(str => g.from(str :: HNil))

  implicit val zonedDateTimeOptionalMillisE: Encoder[ZonedDateTime] =
    stringEncoder(ZonedDateTimeParser.format)

  implicit val zonedDateTimeOptionalMillisD: Decoder[ZonedDateTime] =
    Decoder.decodeString.emap { x =>
      ZonedDateTimeParser.parseOpt(x).toRight("Expected DateTime conforming to pattern " +
        "specified in OCPI 2.1 section 14.2, but got " + x)
    }

  implicit val localTimeE: Encoder[LocalTime] = stringEncoder(LocalTimeParser.format)
  implicit val localTimeD: Decoder[LocalTime] = tryStringDecoder(LocalTimeParser.parse)

  implicit val localDateE: Encoder[LocalDate] = stringEncoder(LocalDateParser.format)
  implicit val localDateD: Decoder[LocalDate] = tryStringDecoder(LocalDateParser.parse)

  implicit val durationE: Encoder[Duration] = Encoder.encodeLong.contramap[Duration](_.getSeconds)
  implicit val durationD: Decoder[Duration] = Decoder.decodeLong.emap(n =>
    java.time.Duration.ofSeconds(n).asRight
  )

  implicit val countryCodeE: Encoder[CountryCode] = stringEncoder(_.value)
  implicit val countryCodeD: Decoder[CountryCode] = tryStringDecoder(CountryCode.apply)

  implicit val languageE: Encoder[Language] = stringEncoder(_.value)
  implicit val languageD: Decoder[Language] = tryStringDecoder(Language.apply)

  implicit def statusCodeE[T <: OcpiStatusCode : ClassTag]: Encoder[T] = Encoder.encodeInt.contramap[T](_.code)
  implicit def statusCodeD[T <: OcpiStatusCode : ClassTag]: Decoder[T] = Decoder.decodeInt.emap[T] { x =>
    OcpiStatusCode(x) match {
      case y: T => y.asRight
      case _ => s"StatusCode $x is not of type ${classTag[T].runtimeClass.getSimpleName}".asLeft
    }
  }

  implicit val currencyCodeE: Encoder[CurrencyCode] = stringEncoder(_.value)
  implicit val currencyCodeD: Decoder[CurrencyCode] = tryStringDecoder(CurrencyCode.apply)

  implicit val successRespUnitE: Encoder[SuccessResp[Unit]] =
    Encoder.forProduct3("status_code", "status_message", "timestamp")(x =>
      (x.statusCode, x.statusMessage, x.timestamp
    )
  )

  implicit val successRespUnitD: Decoder[SuccessResp[Unit]] =
    Decoder.forProduct3("status_code", "status_message", "timestamp") {
      (statusCode: SuccessCode, statusMsg: Option[String], timestamp: ZonedDateTime) =>
        SuccessResp(statusCode, statusMsg, timestamp)
    }

  private def successPagedRespWithoutDataD[T]: Decoder[SuccessResp[Iterable[T]]] =
    implicitly[Decoder[SuccessResp[Unit]]].map(_.copy(data = Iterable.empty[T]))

  private def successPagedRespWithData[T: Decoder]: Decoder[SuccessResp[Iterable[T]]] = successRespD[Iterable[T]]

  def successPagedResp[T: Decoder]: Decoder[SuccessResp[Iterable[T]]] =
    successPagedRespWithData[T].handleErrorWith {
      case e if e.history == List(DownField("data")) => successPagedRespWithoutDataD[T]
    }

  implicit val errorRespE: Encoder[ErrorResp] = deriveEncoder
  implicit val errorRespD: Decoder[ErrorResp] = deriveDecoder

  implicit def successRespE[D : Encoder]: Encoder[SuccessResp[D]] = deriveEncoder
  implicit def successRespD[D : Decoder]: Decoder[SuccessResp[D]] = deriveDecoder


  implicit val displayTextE: Encoder[DisplayText] = deriveEncoder
  implicit val displayTextD: Decoder[DisplayText] = deriveDecoder

  implicit val imageE: Encoder[Image] = deriveEncoder
  implicit val imageD: Decoder[Image] = deriveDecoder

  implicit val businessDetailsE: Encoder[BusinessDetails] = deriveEncoder
  implicit val businessDetailsD: Decoder[BusinessDetails] = deriveDecoder
}

object CommonJsonProtocol extends CommonJsonProtocol
