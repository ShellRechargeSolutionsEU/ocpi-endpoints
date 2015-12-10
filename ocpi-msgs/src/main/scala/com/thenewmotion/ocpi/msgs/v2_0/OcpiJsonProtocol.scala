package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.money._
import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails, LocalizedText, _}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{CredsResp, Creds}
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import com.thenewmotion.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import spray.json._


object OcpiJsonProtocol extends DefaultJsonProtocol {

  implicit val dateTimeOptionalMillisFormat = new JsonFormat[DateTime] {
    val formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
    val formatter = ISODateTimeFormat.dateTime.withZoneUTC
    def write (x: DateTime) = JsString (formatterNoMillis.print (x) )
    def read (value: JsValue) = value match {
      case JsString (x) => formatterNoMillis.parseOption (x) match {
        case Some(parsed) => parsed
        case None => formatter.parseDateTime(x)
      }
      case x => deserializationError ("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit val currencyUnitFormat = new JsonFormat[CurrencyUnit] {
    def write(x: CurrencyUnit) = JsString(x.getCode)
    def read(value: JsValue) = value match {
      case JsString(x) => CurrencyUnit(x)
      case x => deserializationError("Expected CurrencyUnit as JsString, but got " + x)
    }
  }


  implicit val moneyFormat = new JsonFormat[Money] {
    def write(x: Money) = JsObject(
      "currency" -> JsString(x.getCurrencyUnit.toString),
      "amount" -> JsString(x.getAmount.toString)
    )

    def read(value: JsValue) = {
      def err(x: Any) = deserializationError( """Expected Money as ex.: {"currency": "String", "amount": "String"}, but got """ + x)
      value match {
        case JsObject(l) => l.toList match {
          case List(("currency", JsString(cur)), ("amount", JsString(am))) =>
            MoneyFromBigDecimal(BigDecimal(am).bigDecimal).of(CurrencyUnit(cur))
          case x => err(x)
        }
        case x => err(x)
      }
    }
  }

  implicit val capabilityFormat =
    new SimpleStringEnumSerializer[Capability](CapabilityEnum).enumFormat

  implicit val connectorStatusFormat =
    new SimpleStringEnumSerializer[ConnectorStatus](ConnectorStatusEnum).enumFormat

  implicit val connectorTypeFormat =
    new SimpleStringEnumSerializer[ConnectorType](ConnectorTypeEnum).enumFormat

  implicit val connectorFormatFormat =
    new SimpleStringEnumSerializer[ConnectorFormat](ConnectorFormatEnum).enumFormat

  implicit val currentTypeFormat =
    new SimpleStringEnumSerializer[CurrentType](CurrentTypeEnum).enumFormat

  implicit val pricingUnitFormat =
    new SimpleStringEnumSerializer[PricingUnit](PricingUnitEnum).enumFormat

  implicit val periodTypeFormat =
    new SimpleStringEnumSerializer[PeriodType](PeriodTypeEnum).enumFormat

  implicit val locationTypeFormat =
    new SimpleStringEnumSerializer[LocationType](LocationTypeEnum).enumFormat



  implicit val displayTextFormat = jsonFormat2(LocalizedText)
  implicit val powerFormat = jsonFormat3(Power)
  implicit val tariffFormat = jsonFormat8(Tariff)
  implicit val priceSchemeFormat = jsonFormat5(PriceScheme)
  implicit val geoLocationFormat = jsonFormat2(GeoLocation)
  implicit val regularHoursFormat = jsonFormat3(RegularHours)
  implicit val exceptionalPeriodFormat = jsonFormat2(ExceptionalPeriod)
  implicit val hoursFormat = jsonFormat4(Hours)
  implicit val businessDetailsFormat = jsonFormat3(BusinessDetails)
  implicit val parkingRestrictionsFormat = jsonFormat0(ParkingRestriction)
  implicit val imageFormat = jsonFormat0(Image)

  implicit val connectorFormat = jsonFormat8(Connector)
  implicit val evseFormat = jsonFormat11(Evse)
  implicit val operatorFormat = jsonFormat3(Operator)
  implicit val locationFormat = jsonFormat14(Location)
  implicit val locationsDataFormat = new RootJsonFormat[LocationsData] {

    def write(x: LocationsData) = {
      val evses = x.locations.flatMap(_.evses).flatten
      JsObject(
        "locations" -> x.locations.map(_.copy(evses = None)).toJson,
        "evses" -> evses.toJson
      )
    }
    def read(value: JsValue) = value.asJsObject.getFields("locations", "evses") match {
      case Seq(JsArray(locationVals), JsArray(evseVals)) =>
        val evses = evseVals.toList.map(_.convertTo[Evse])
        LocationsData(
          locationVals.toList.map(loc =>
            {val loco = loc.convertTo[Location];loco.copy(evses =
              Some(evses.filter(_.location_id == loco.id)))})
        )
      case x => deserializationError("Expected LocationType as JsString, but got " + x)
    }
  }
  implicit val locationRespFormat = jsonFormat4(LocationResp)


  implicit val versionFormat = jsonFormat2(Version)
  implicit val versionsRespFormat = jsonFormat4(VersionsResp)

  implicit val endpointIdentifierFormat =
    new SimpleStringEnumSerializer[EndpointIdentifier](EndpointIdentifier).enumFormat
  implicit val endpointFormat = jsonFormat2(Endpoint)
  implicit val versionDetailsFormat = jsonFormat2(VersionDetails)
  implicit val versionDetailsRespFormat = jsonFormat4(VersionDetailsResp)
  implicit val errorRespFormat = jsonFormat3(ErrorResp)
  implicit val successRespFormat = jsonFormat3(SuccessResp)
  implicit val credentialsFormat = jsonFormat3(Creds)
  implicit val credentialsRespFormat = jsonFormat4(CredsResp)
}
