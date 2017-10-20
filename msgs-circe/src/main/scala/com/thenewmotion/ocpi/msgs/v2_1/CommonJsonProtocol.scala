package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}

import com.thenewmotion.ocpi.{LocalDateParser, LocalTimeParser, ZonedDateTimeParser}
import io.circe.{Decoder, Encoder}
import cats.syntax.either._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.SuccessCode
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{BusinessDetails, DisplayText, Image, ImageCategory}
import io.circe.generic.extras.semiauto._
import scala.reflect.{ClassTag, classTag}
import scala.util.Try

trait CommonJsonProtocol {

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

  implicit val imageCategoryTypeE: Encoder[ImageCategory] =
    SimpleStringEnumSerializer.encoder(ImageCategory)

  implicit val imageCategoryTypeD: Decoder[ImageCategory] =
    SimpleStringEnumSerializer.decoder(ImageCategory)

  implicit val urlE: Encoder[Url] = Encoder.encodeString.contramap[Url](_.value)
  implicit val urlD: Decoder[Url] = Decoder.decodeString.emap(Url(_).asRight)

  implicit val countryCodeE: Encoder[CountryCode] = stringEncoder(_.value)
  implicit val countryCodeD: Decoder[CountryCode] = Decoder.decodeString.emap { x =>
    Try(CountryCode(x)).toEither.leftMap(_.getMessage)
  }

  implicit val languageE: Encoder[Language] = Encoder.encodeString.contramap[Language](_.value)
  implicit val languageD: Decoder[Language] = tryStringDecoder(Language.apply)

  implicit def statusCodeE[T <: OcpiStatusCode : ClassTag]: Encoder[T] = Encoder.encodeInt.contramap[T](_.code)
  implicit def statusCodeD[T <: OcpiStatusCode : ClassTag]: Decoder[T] = Decoder.decodeInt.emap[T] { x =>
    OcpiStatusCode(x) match {
      case y: T => y.asRight
      case _ => s"StatusCode $x is not of type ${classTag[T].runtimeClass.getSimpleName}".asLeft
    }
  }

  implicit def authTokenE[O <: Ownership]: Encoder[AuthToken[O]] = Encoder.encodeString.contramap[AuthToken[O]](_.value)
  implicit def authTokenD[O <: Ownership]: Decoder[AuthToken[O]] = tryStringDecoder(AuthToken.apply[O])

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
