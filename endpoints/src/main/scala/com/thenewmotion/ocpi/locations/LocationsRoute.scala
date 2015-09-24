package com.thenewmotion.ocpi
package locations

import com.thenewmotion.money.CurrencyUnit
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.LocalizedText
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.routing.Route

class LocationsRoute(currentTime: => DateTime = DateTime.now) extends JsonApi {
  def route(version: Version, authToken: AuthToken): Route = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    get {
      complete(LocationResp(GenericSuccess.code, None, currentTime,
        LocationsData(List(TestLocation.location1))))
    }
  }
}

object TestLocation{

  val displayText_standard = List(LocalizedText(Some("nl"), Some("Standaard Tarief")),
    LocalizedText(Some("en"), Some("Standard Tariff")) )
  val displayText_emsp =  List(LocalizedText(Some("nl"), Some("eMSP Tarief")),
    LocalizedText(Some("en"), Some("eMSP Tariff")) )

  val tariff_standard = Tariff(
    tariff_id = "kwrate",
    price_untaxed = Some(0.1936),
    price_taxed = None,
    pricing_unit = PricingUnitEnum.KWhToEV,
    tax_pct = None,
    currency = CurrencyUnit("EUR"),
    condition = None,
    display_text = displayText_standard
  )

  val tariff_emsp = Tariff(
    tariff_id = "kwrate",
    price_untaxed = Some(0.1536),
    price_taxed = None,
    pricing_unit = PricingUnitEnum.KWhToEV,
    tax_pct = None,
    currency = CurrencyUnit("EUR"),
    condition = None,
    display_text = displayText_emsp
  )


  val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
  val date1 = formatter.parseDateTime("2010-01-01T00:00:00Z")
  val date2 = formatter.parseDateTime("2020-12-31T23:59:59Z")



  val priceScheme1 = PriceScheme(
    1,
    Some(date1),
    Some(date2),
    Some(List(tariff_standard)),
    displayText_standard
  )

  val priceScheme2 = PriceScheme(
    2,
    Some(date1),
    Some(date2),
    Some(List(tariff_emsp)),
    displayText_emsp
  )

  val connector1 = Connector(
    "1",
    ConnectorTypeEnum.`IEC-62196-T2`,
    ConnectorFormatEnum.Cable,
    price_schemes = Some(List(priceScheme1,priceScheme2)),
    CurrentTypeEnum.AC3Phases,
    220,
    16
  )

  val connector2 = Connector(
    "2",
    ConnectorTypeEnum.`IEC-62196-T2`,
    ConnectorFormatEnum.Socket,
    price_schemes = Some(List(priceScheme1)),
    CurrentTypeEnum.AC3Phases,
    220,
    16
  )

  val evse1 = Evse(
    "NL-TNM-E02000818",
    "LOC1",
    ConnectorStatusEnum.Available,
    Some(List("RESERVABLE")),
    List(connector1,connector2),
    floor_level = Some("-1"),
    physical_number = Some(1)
  )
  val location1 = Location(
    "LOC1",
    `type` = LocationTypeEnum.OnStreet,
    None,
    address = "Keizersgracht 585",
    city = "Amsterdam",
    postal_code = "1017DR",
    country = "NL",
    coordinates = GeoLocation("4.891792500000065", "52.3642069"),
    evses = Some(List(evse1)),
    directions = Some("left, left, left, right, left")

  )
}