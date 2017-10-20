package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{LocalTime, ZoneId, ZonedDateTime}

import com.thenewmotion.ocpi.msgs.v2_1.Tariffs.DayOfWeek._
import com.thenewmotion.ocpi.msgs.v2_1.Tariffs.TariffDimensionType.{Flat, ParkingTime, Time}
import com.thenewmotion.ocpi.msgs.v2_1.Tariffs._
import com.thenewmotion.ocpi.msgs.{CurrencyCode, Url}
import org.specs2.specification.core.Fragments
import scala.language.higherKinds

trait GenericTariffsSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit tariffR: GenericJsonReader[Tariff],
    tariffW: GenericJsonWriter[Tariff]
  ): Fragments = {
    "Tariff" should {
      testPair(tariff, parse(tariffJson))
    }
  }

  val tariff =
    Tariff(
      id = TariffId("11"),
      currency = CurrencyCode("EUR"),
      tariffAltUrl = Some(Url("https://company.com/tariffs/11")),
      elements = List(
        TariffElement(
          List(PriceComponent(Flat, BigDecimal("2.50"), 1)),
          None
        ),
        TariffElement(
          List(PriceComponent(Time, BigDecimal("1.00"), 900)),
          Some(TariffRestrictions(maxPower = Some(BigDecimal("32.00"))))
        ),
        TariffElement(
          List(PriceComponent(Time, BigDecimal("2.00"), 600)),
          Some(TariffRestrictions(minPower = Some(BigDecimal("32.00")), dayOfWeek = Some(List(Monday, Tuesday, Wednesday, Thursday, Friday))))
        ),
        TariffElement(
          List(PriceComponent(Time, 1.25, 600)),
          Some(TariffRestrictions(minPower = Some(BigDecimal("32.00")), dayOfWeek = Some(List(Saturday, Sunday))))
        ),
        TariffElement(
          List(PriceComponent(ParkingTime, BigDecimal("5.00"), 300)),
          Some(TariffRestrictions(startTime = Some(LocalTime.of(9, 0)), endTime = Some(LocalTime.of(18, 0)), dayOfWeek = Some(List(Monday, Tuesday, Wednesday, Thursday, Friday))))
        ),
        TariffElement(
          List(PriceComponent(ParkingTime, BigDecimal("6.00"), 300)),
          Some(TariffRestrictions(startTime = Some(LocalTime.of(10, 0)), endTime = Some(LocalTime.of(17, 0)), dayOfWeek = Some(List(Saturday))))
        )
      ),
      energyMix = None,
      lastUpdated = ZonedDateTime.of(2015, 6, 29, 20, 39, 9, 0, ZoneId.of("Z"))
    )

  val tariffJson =
    """
      |{
      |	"id": "11",
      |	"currency": "EUR",
      |	"tariff_alt_url": "https://company.com/tariffs/11",
      |	"elements": [{
      |		"price_components": [{
      |			"type": "FLAT",
      |			"price": 2.50,
      |			"step_size": 1
      |		}]
      |	}, {
      |		"price_components": [{
      |			"type": "TIME",
      |			"price": 1.00,
      |			"step_size": 900
      |		}],
      |		"restrictions": {
      |			"max_power": 32.00
      |		}
      |	}, {
      |		"price_components": [{
      |			"type": "TIME",
      |			"price": 2.00,
      |			"step_size": 600
      |		}],
      |		"restrictions": {
      |			"min_power": 32.00,
      |			"day_of_week": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
      |		}
      |	}, {
      |		"price_components": [{
      |			"type": "TIME",
      |			"price": 1.25,
      |			"step_size": 600
      |		}],
      |		"restrictions": {
      |			"min_power": 32.00,
      |			"day_of_week": ["SATURDAY", "SUNDAY"]
      |		}
      |	}, {
      |		"price_components": [{
      |			"type": "PARKING_TIME",
      |			"price": 5.00,
      |			"step_size": 300
      |		}],
      |		"restrictions": {
      |			"start_time": "09:00",
      |			"end_time": "18:00",
      |			"day_of_week": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
      |		}
      |	}, {
      |		"price_components": [{
      |			"type": "PARKING_TIME",
      |			"price": 6.00,
      |			"step_size": 300
      |		}],
      |		"restrictions": {
      |			"start_time": "10:00",
      |			"end_time": "17:00",
      |			"day_of_week": ["SATURDAY"]
      |		}
      |	}],
      |	"last_updated": "2015-06-29T20:39:09Z"
      |}
    """.stripMargin

}
