package com.thenewmotion.ocpi.msgs.v2_1

import java.time.ZonedDateTime
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs._
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.v2_1.Tariffs._
import com.thenewmotion.ocpi.msgs.{CountryCode, CurrencyCode}
import org.specs2.specification.core.Fragments
import scala.language.higherKinds

trait GenericCdrsSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit cdrR: GenericJsonReader[Cdr],
    cdrW: GenericJsonWriter[Cdr]
  ): Fragments = {
    "Cdrs" should {
      testPair(cdr, parse(cdrJson))
    }
  }

  val cdrJson =
    """
      |{
      |	"id": "12345",
      |	"start_date_time": "2015-06-29T21:39:09Z",
      |	"stop_date_time": "2015-06-29T23:37:32Z",
      |	"auth_id": "DE8ACC12E46L89",
      |	"auth_method": "WHITELIST",
      |	"location": {
      |		"id": "LOC1",
      |		"type": "ON_STREET",
      |		"name": "Gent Zuid",
      |		"address": "F.Rooseveltlaan 3A",
      |		"city": "Gent",
      |		"postal_code": "9000",
      |		"country": "BEL",
      |   "directions": [],
      |		"coordinates": {
      |			"latitude": "3.72994",
      |			"longitude": "51.04759"
      |		},
      |		"evses": [{
      |			"uid": "3256",
      |			"evse_id": "BE-BEC-E041503003",
      |			"status": "AVAILABLE",
      |     "capabilities": [],
      |     "parking_restrictions": [],
      |     "status_schedule": [],
      |     "images": [],
      |     "directions": [],
      |			"connectors": [{
      |				"id": "1",
      |				"standard": "IEC_62196_T2",
      |				"format": "SOCKET",
      |				"power_type": "AC_1_PHASE",
      |				"voltage": 230,
      |				"amperage": 64,
      |				"tariff_id": "11",
      |				"last_updated": "2015-06-29T21:39:01Z"
      |			}],
      |			"last_updated": "2015-06-29T21:39:01Z"
      |		}],
      |		"last_updated": "2015-06-29T21:39:01Z",
      |   "facilities": [],
      |   "images": [],
      |   "related_locations": []
      |	},
      |	"currency": "EUR",
      |	"tariffs": [{
      |		"id": "12",
      |		"currency": "EUR",
      |		"elements": [{
      |			"price_components": [{
      |				"type": "TIME",
      |				"price": 2.00,
      |				"step_size": 300
      |			}]
      |		}],
      |		"last_updated": "2015-02-02T14:15:01Z"
      | }],
      |	"charging_periods": [{
      |		"start_date_time": "2015-06-29T21:39:09Z",
      |		"dimensions": [{
      |			"type": "TIME",
      |			"volume": 1.973
      |		}]
      |	}],
      |	"total_cost": 4.00,
      |	"total_energy": 15.342,
      |	"total_time": 1.973,
      |	"last_updated": "2015-06-29T22:01:13Z"
      |}
    """.stripMargin

  val cdr = Cdr(
    id = CdrId("12345"),
    startDateTime = ZonedDateTime.parse("2015-06-29T21:39:09Z"),
    stopDateTime = ZonedDateTime.parse("2015-06-29T23:37:32Z"),
    authId = "DE8ACC12E46L89",
    authMethod = AuthMethod.Whitelist,
    location = Location(
      LocationId("LOC1"),
      ZonedDateTime.parse("2015-06-29T21:39:01Z"),
      LocationType.OnStreet,
      Some("Gent Zuid"),
      "F.Rooseveltlaan 3A",
      "Gent",
      "9000",
      CountryCode("BEL"),
      GeoLocation("3.72994", "51.04759"),
      List(),
      List(
        Evse(EvseUid("3256"), ZonedDateTime.parse("2015-06-29T21:39:01Z"), ConnectorStatus.Available,
          List(
            Connector(
              ConnectorId("1"),
              ZonedDateTime.parse("2015-06-29T21:39:01Z"),
              ConnectorType.`IEC_62196_T2`,
              ConnectorFormat.Socket,
              PowerType.AC1Phase,
              230,
              64,
              Some("11")
            )
          ),
          evseId = Some("BE-BEC-E041503003")
        )
      ),
      chargingWhenClosed = None
    ),
    currency = CurrencyCode("EUR"),
    tariffs = Some(List(
      Tariff(
        TariffId("12"),
        CurrencyCode("EUR"),
        elements = List(TariffElement(List(PriceComponent(TariffDimensionType.Time, BigDecimal("2.00"), 300)))),
        lastUpdated = ZonedDateTime.parse("2015-02-02T14:15:01Z"))
    )),
    chargingPeriods = List(
      ChargingPeriod(ZonedDateTime.parse("2015-06-29T21:39:09Z"), List(CdrDimension(CdrDimensionType.Time, 1.973)))
    ),
    totalCost = BigDecimal("4.00"),
    totalEnergy = 15.342,
    totalTime = 1.973,
    lastUpdated = ZonedDateTime.parse("2015-06-29T22:01:13Z")
  )

}
