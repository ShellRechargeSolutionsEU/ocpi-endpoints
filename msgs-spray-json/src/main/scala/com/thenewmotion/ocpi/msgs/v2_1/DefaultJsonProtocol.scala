package com.thenewmotion.ocpi
package msgs.v2_1

import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._

import msgs.OcpiStatusCode.SuccessCode
import msgs._
import msgs.v2_1.CommonTypes.{BusinessDetails, DisplayText, Image, ImageCategory}
import spray.json.{JsNumber, JsString, JsValue, JsonFormat, deserializationError}

trait DefaultJsonProtocol extends spray.json.DefaultJsonProtocol {

  import reflect._

  private val PASS1 = """([A-Z]+)([A-Z][a-z])""".r
  private val PASS2 = """([a-z\d])([A-Z])""".r
  private val REPLACEMENT = "$1_$2"

  // Convert camelCase to snake_case
  override def extractFieldNames(classTag: ClassTag[_]): Array[String] = {
    import java.util.Locale

    def snakify(name: String) = PASS2.replaceAllIn(PASS1.replaceAllIn(name, REPLACEMENT), REPLACEMENT).toLowerCase(Locale.US)

    super.extractFieldNames(classTag) map snakify
  }


  implicit val ZonedDateTimeOptionalMillisFormat = new JsonFormat[ZonedDateTime] {
    def write (x: ZonedDateTime) = JsString(ZonedDateTimeParser.format(x))
    def read (value: JsValue) = value match {
      case JsString (x) => ZonedDateTimeParser.parseOpt(x) match {
        case Some(parsed) => parsed
        case None => deserializationError ("Expected DateTime conforming to pattern " +
          "specified in OCPI 2.1 section 14.2, but got " + x)
      }
      case x => deserializationError ("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit object localTimeJsonFormat extends JsonFormat[LocalTime] {
    def write(c: LocalTime) = JsString(LocalTimeParser.format(c))
    def read(value: JsValue) = value match {
      case JsString(str) => LocalTimeParser.parse(str)
      case x => deserializationError("Expected Time as String, but got " + x)
    }
  }

  implicit object localDateJsonFormat extends JsonFormat[LocalDate] {
    def write(c: LocalDate) = JsString(LocalDateParser.format(c))
    def read(value: JsValue) = value match {
      case JsString(str) => LocalDateParser.parse(str)
      case x => deserializationError("Expected Date as String, but got " + x)
    }
  }

  implicit val durationFormat = new JsonFormat[Duration] {
    override def write(obj: Duration) = JsNumber(obj.getSeconds)

    override def read(json: JsValue) = json match {
      case JsNumber(n) => java.time.Duration.ofSeconds(n.toLong)
      case x => deserializationError(s"Expected Duration as JsNumber, but got $x")
    }
  }

  implicit val imageCategoryTypeFormat =
    new SimpleStringEnumSerializer[ImageCategory](ImageCategory).enumFormat

  implicit val urlFmt = new JsonFormat[Url] {
    override def read(json: JsValue) = json match {
      case JsString(s) => Url(s)
      case _ => deserializationError("Url must be a string")
    }
    override def write(obj: Url) = JsString(obj.value)
  }
  implicit val imageFormat = jsonFormat6(Image)
  implicit val businessDetailsFormat = jsonFormat3(BusinessDetails)

  implicit val countryCodeFmt = new JsonFormat[CountryCode] {
    override def read(json: JsValue) = json match {
      case JsString(s) => CountryCode(s)
      case _ => deserializationError("Country Code must be a string")
    }
    override def write(obj: CountryCode) = JsString(obj.value)
  }
  implicit val languageFmt = new JsonFormat[Language] {
    override def read(json: JsValue) = json match {
      case JsString(s) => Language(s)
      case _ => deserializationError("Language must be a string")
    }
    override def write(obj: Language) = JsString(obj.value)
  }

  implicit def statusCodeFormat[T <: OcpiStatusCode : ClassTag] = new JsonFormat[T] {
    override def read(json: JsValue): T = json match {
      case JsNumber(x) => OcpiStatusCode(x.toInt) match {
        case y: T => y
        case _ => deserializationError(s"StatusCode $x is not of type ${classTag[T].runtimeClass.getSimpleName}")
      }
      case x => deserializationError("Expected StatusCode as JsNumber, but got " + x)
    }
    override def write(obj: T): JsValue = JsNumber(obj.code)
  }

  implicit def authTokenFormat[O <: Ownership] = new JsonFormat[AuthToken[O]] {
    override def read(json: JsValue) = json match {
      case JsString(x) => AuthToken[O](x)
      case x => deserializationError(s"Expected AuthToken as JsString, but got $x")
    }
    override def write(obj: AuthToken[O]) = JsString(obj.value)
  }

  implicit val errorRespFormat = jsonFormat3(ErrorResp)

  implicit val successRespUnitFormat = {
    val Array(p1, p2, p3, _) = extractFieldNames(classTag[SuccessResp[Unit]])
    jsonFormat((statusCode: SuccessCode, statusMsg: Option[String], timeStamp: ZonedDateTime) =>
      SuccessResp(statusCode, statusMsg, timeStamp, ()), p1, p2, p3)
  }

  implicit def successRespFormat[D : JsonFormat] = jsonFormat4(SuccessResp[D])

  implicit val currencyCodeFormat = new JsonFormat[CurrencyCode] {
    override def write(obj: CurrencyCode) = JsString(obj.value)

    override def read(json: JsValue) = json match {
      case JsString(s) => CurrencyCode(s)
      case x => deserializationError(s"Expected CurrencyCode as JsString, but got $x")
    }
  }

  implicit val displayTextFormat = jsonFormat2(DisplayText)
}

object DefaultJsonProtocol extends DefaultJsonProtocol