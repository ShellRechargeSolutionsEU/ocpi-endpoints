package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.money._
import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails, _}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{Creds, CredsResp}
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
    new SimpleStringEnumSerializer[Capability](Capability).enumFormat

  implicit val connectorStatusFormat =
    new SimpleStringEnumSerializer[ConnectorStatus](ConnectorStatus).enumFormat

  implicit val connectorTypeFormat =
    new SimpleStringEnumSerializer[ConnectorType](ConnectorType).enumFormat

  implicit val connectorFormatFormat =
    new SimpleStringEnumSerializer[ConnectorFormat](ConnectorFormat).enumFormat

  implicit val currentTypeFormat =
    new SimpleStringEnumSerializer[PowerType](PowerType).enumFormat

  implicit val pricingUnitFormat =
    new SimpleStringEnumSerializer[PricingUnit](PricingUnit).enumFormat

  implicit val periodTypeFormat =
    new SimpleStringEnumSerializer[PeriodType](PeriodType).enumFormat

  implicit val locationTypeFormat =
    new SimpleStringEnumSerializer[LocationType](LocationType).enumFormat

  implicit val parkingRestrictionTypeFormat =
    new SimpleStringEnumSerializer[ParkingRestriction](ParkingRestriction).enumFormat

  implicit val imageCategoryTypeFormat =
    new SimpleStringEnumSerializer[ImageCategory](ImageCategory).enumFormat

  implicit val powerFormat = jsonFormat3(Power)
  implicit val displayTestFormat = jsonFormat2(DisplayText)
  implicit val tariffFormat = jsonFormat8(Tariff)
  implicit val priceSchemeFormat = jsonFormat5(PriceScheme)
  implicit val geoLocationFormat = jsonFormat2(GeoLocation)
  implicit val additionalGeoLocationFormat = jsonFormat3(AdditionalGeoLocation)
  implicit val regularHoursFormat = jsonFormat3(RegularHours)
  implicit val exceptionalPeriodFormat = jsonFormat2(ExceptionalPeriod)
  implicit val hoursFormat = jsonFormat4(Hours)
  implicit val imageFormat = jsonFormat6(Image)
  implicit val businessDetailsFormat = jsonFormat3(BusinessDetails)

  private def jsNumToStr(jsVal: JsValue) = jsVal match {
    case JsNumber(x) => x.toString
    case JsString(x) => x
    case x => deserializationError(s"Expected JsNumber or JsString; got $x")
  }

  private def jsNumToOptStr(jsVal: JsValue) = jsVal match {
    case JsNumber(x) => Some(x.toString)
    case JsString(x) => Some(x)
    case _      => None
  }

  private def jsNullToEmptyArray(jsVal: JsValue) = jsVal match {
    case JsNull => JsArray()
    case x: JsArray => x
    case x => deserializationError(s"Expected JsNull or JsArray; got $x")
  }

  implicit val connectorFormat = new RootJsonFormat[Connector] {
    def write (x: Connector) = jsonFormat9(Connector).write(x)
    def read (value: JsValue): Connector = {
      val fieldNames = Seq("id", "status", "standard", "format", "power_type",
        "voltage", "amperage", "tariff_id", "terms_and_conditions")
      fieldNames.map(fn =>
        value.asJsObject.fields.getOrElse(fn, JsNull)) match {
        case Seq(id, status, standard, format,
            power_type, JsNumber(voltage), JsNumber(amperage), tariff_id,
            terms_and_conditions) =>
          Connector(jsNumToStr(id), status.convertTo[Option[ConnectorStatus]] getOrElse ConnectorStatus.Unknown,
            standard.convertTo[ConnectorType], format.convertTo[ConnectorFormat],
            power_type.convertTo[PowerType], voltage.toInt, amperage.toInt,
            jsNumToOptStr(tariff_id), terms_and_conditions.convertTo[Option[Url]])
        case x => deserializationError(s"Connector object expected; got $x")
      }
    }
  }

  implicit val connectorPatchFormat = new RootJsonFormat[ConnectorPatch] {
    def write (x: ConnectorPatch) = jsonFormat9(ConnectorPatch).write(x)
    def read (value: JsValue): ConnectorPatch = {
      val fieldNames = Seq("id", "status", "standard", "format", "power_type",
        "voltage", "amperage", "tariff_id", "terms_and_conditions")
      fieldNames.map(fn =>
        value.asJsObject.fields.getOrElse(fn, JsNull)) match {
        case Seq(id, status, standard, format,
        power_type, voltage, amperage, tariff_id,
        terms_and_conditions) =>
          ConnectorPatch(jsNumToStr(id), status.convertTo[Option[ConnectorStatus]],
            standard.convertTo[Option[ConnectorType]], format.convertTo[Option[ConnectorFormat]],
            power_type.convertTo[Option[PowerType]], voltage.convertTo[Option[Int]], amperage.convertTo[Option[Int]],
            jsNumToOptStr(tariff_id), terms_and_conditions.convertTo[Option[Url]])
        case x => deserializationError(s"Connector object expected; got $x")
      }
    }
  }

  implicit val statusScheduleFormat = jsonFormat3(StatusSchedule)

  implicit val evseFormat = new RootJsonFormat[Evse] {
    def write(x: Evse) = jsonFormat12(Evse).write(x)
    def read(value: JsValue): Evse = {
      val fieldNames = Seq("uid", "status", "connectors", "status_schedule", "capabilities",
        "evse_id", "floor_level", "coordinates", "physical_reference", "directions",
        "parking_restrictions", "images")
      fieldNames.map(fn =>
        value.asJsObject.fields.getOrElse(fn, JsNull)) match {
        case Seq(JsString(uid), status, connectors, status_schedule, capabilities,
        evse_id, floor_level, coordinates, physical_reference, directions,
        parking_restrictions, images) =>
          Evse(uid, status.convertTo[ConnectorStatus], connectors.convertTo[List[Connector]],
            jsNullToEmptyArray(status_schedule).convertTo[List[StatusSchedule]],
            jsNullToEmptyArray(capabilities).convertTo[List[Capability]],
            evse_id.convertTo[Option[String]], floor_level.convertTo[Option[String]],
            coordinates.convertTo[Option[GeoLocation]], physical_reference.convertTo[Option[String]],
            jsNullToEmptyArray(directions).convertTo[List[DisplayText]],
            jsNullToEmptyArray(parking_restrictions).convertTo[List[ParkingRestriction]],
            jsNullToEmptyArray(images).convertTo[List[Image]])
        case x => deserializationError(s"Connector object expected; got $x")
      }
    }
  }
  implicit val evsePatchFormat = jsonFormat12(EvsePatch)
  implicit val operatorFormat = jsonFormat3(Operator)

  implicit val locationFormat = new RootJsonFormat[Location] {
    def write(x: Location) = jsonFormat16(Location).write(x)
    def read(value: JsValue): Location = {

      val fieldNames = Seq("id", "type", "name", "address", "city",
        "postal_code", "country", "coordinates", "related_locations", "evses",
        "directions", "operator", "suboperator", "opening_times", "charging_when_closed", "images")
      fieldNames.map(fn =>
        value.asJsObject.fields.getOrElse(fn, JsNull)) match {
        case Seq(JsString(id), _type, name, address, city,
        postal_code, country, coordinates, related_locations, evses,
        directions, operator, suboperator, opening_times, charging_when_closed, images) =>
          Location(id, _type.convertTo[LocationType], name.convertTo[Option[String]], address.convertTo[String],
            city.convertTo[String], postal_code.convertTo[String], country.convertTo[String],
            coordinates.convertTo[GeoLocation],
            jsNullToEmptyArray(related_locations).convertTo[List[AdditionalGeoLocation]],
            evses.convertTo[List[Evse]], jsNullToEmptyArray(directions).convertTo[List[DisplayText]],
            operator.convertTo[Option[BusinessDetails]], suboperator.convertTo[Option[BusinessDetails]],
            opening_times.convertTo[Option[Hours]], charging_when_closed.convertTo[Option[Boolean]],
            jsNullToEmptyArray(images).convertTo[List[Image]])
        case x => deserializationError(s"Connector object expected; got $x")
      }
    }
  }

  implicit val locationPatchFormat = jsonFormat16(LocationPatch)
  implicit val locationsRespFormat = jsonFormat4(LocationsResp)
  implicit val locationRespFormat = jsonFormat4(LocationResp)
  implicit val evseRespFormat = jsonFormat4(EvseResp)
  implicit val connectorRespFormat = jsonFormat4(ConnectorResp)
  implicit val versionFormat = jsonFormat2(Version)
  implicit val versionsRespFormat = jsonFormat4(VersionsResp)
  implicit val versionsReqFormat = jsonFormat5(VersionsRequest)

  implicit val endpointIdentifierFormat =
    new SimpleStringEnumSerializer[EndpointIdentifier](EndpointIdentifier).enumFormat
  implicit val endpointFormat = jsonFormat2(Endpoint)
  implicit val versionDetailsFormat = jsonFormat2(VersionDetails)
  implicit val versionDetailsRespFormat = jsonFormat4(VersionDetailsResp)
  implicit val errorRespFormat = jsonFormat3(ErrorResp)
  implicit val successRespFormat = jsonFormat3(SuccessResp)
  implicit val credentialsFormat = jsonFormat5(Creds)
  implicit val credentialsRespFormat = jsonFormat4(CredsResp)
}
