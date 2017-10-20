package com.thenewmotion.ocpi.msgs.v2_1

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.CountryCode
import org.specs2.specification.core.Fragment
import scala.language.higherKinds

trait GenericLocationsSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit evseR: GenericJsonReader[Evse],
    evseW: GenericJsonWriter[Evse],
    locationR: GenericJsonReader[Location],
    energyMixR: GenericJsonReader[EnergyMix],
    energyMixW: GenericJsonWriter[EnergyMix],
    hoursR: GenericJsonReader[Hours],
    hoursW: GenericJsonWriter[Hours]
  ): Fragment = {
    "Evses" should {
      testPair(evse1, parse(evseJson1))

      "deserialize missing fields of cardinality '*' to empty lists" in {
        val evse = parseAs[Evse](evseJson1
          .replaceAll(""""status_schedule": \[\],""", "")
          .replaceAll(""""capabilities": \[[| \nA-Z"]+\],""", "")
          .replaceAll(""""directions": \[\],""", "")
          .replaceAll(""""parking_restrictions": \[\],""", "")
          .replaceAll(""""images": \[\],""", "")
        )

        evse.statusSchedule mustEqual Nil
        evse.capabilities mustEqual Nil
        evse.directions mustEqual Nil
        evse.parkingRestrictions mustEqual Nil
        evse.images mustEqual Nil
      }
    }

    "Location" should {
      "deserialize missing fields of cardinality '*' to empty lists" in {
        val loc = parseAs[Location](locationJson1
          .replaceAll(""""related_locations": \[\],""", "")
          .replaceAll(""""directions": \[[| \na-z:{},"]+\],""", "")
          .replaceAll(""""facilities": \[\],""", "")
          .replaceAll(""""images": \[\],""", "")
        )

        loc.relatedLocations mustEqual Nil
        loc.directions mustEqual Nil
        loc.facilities mustEqual Nil
        loc.images mustEqual Nil
      }
    }

    "EnergyMix" should {
      val emJson = """
                     |{
                     |      "is_green_energy": true,
                     |      "energy_sources": [],
                     |      "environ_impact": [],
                     |      "supplier_name": "Greenpeace Energy eG",
                     |      "energy_product_name": "eco-power"
                     |}
                   """.stripMargin

      testPair(EnergyMix(isGreenEnergy = true, Nil, Nil, Some("Greenpeace Energy eG"), Some("eco-power")), parse(emJson))

      "deserialize missing fields of cardinality '*' to empty lists" in {
        val em = parseAs[EnergyMix]("""
                                      |{
                                      |      "is_green_energy": true,
                                      |      "supplier_name": "Greenpeace Energy eG",
                                      |      "energy_product_name": "eco-power"
                                      |}
                                    """.stripMargin)
        em.energySources mustEqual Nil
        em.environImpact mustEqual Nil
      }
    }

    "Hours" should {
      val hoursJson =
        """
          |{
          | "regular_hours": [],
          | "twentyfourseven": true,
          | "exceptional_openings": [],
          | "exceptional_closings": []
          |}
        """.stripMargin

      testPair(Hours(twentyfourseven = true, Nil, Nil, Nil), parse(hoursJson))

      "deserialize missing fields of cardinality '*' to empty lists" in {
        val hours =
          parseAs[Hours]("""
                           | {
                           |   "twentyfourseven": true
                           | }
                         """.stripMargin)

        hours.exceptionalOpenings mustEqual Nil
        hours.exceptionalClosings mustEqual Nil
      }
    }
  }

  private def parseToUtc(s: String) =
    ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC)

  private val dateOfUpdate = parseToUtc("2016-12-31T23:59:59Z")

  val displayText_standard = List(DisplayText("nl", "Standaard Tarief"),
    DisplayText("en", "Standard Tariff") )
  val displayText_emsp =  List(DisplayText("nl", "eMSP Tarief"),
    DisplayText("en", "eMSP Tariff") )

   val connector1 = Connector(
     ConnectorId("1"),
     lastUpdated = dateOfUpdate,
     ConnectorType.`IEC_62196_T2`,
     ConnectorFormat.Cable,
     PowerType.AC3Phase,
     230,
     16,
     tariffId = Some("kwrate")
   )

  val connector2 = Connector(
    ConnectorId("2"),
    lastUpdated = dateOfUpdate,
    ConnectorType.`IEC_62196_T2`,
    ConnectorFormat.Socket,
    PowerType.AC3Phase,
    230,
    16,
    tariffId = Some("timerate")
  )

  val evse1 = Evse(
    EvseUid("BE-BEC-E041503001"),
    lastUpdated = dateOfUpdate,
    ConnectorStatus.Available,
    capabilities = List(Capability.Reservable),
    connectors = List(connector1, connector2),
    floorLevel = Some("-1"),
    physicalReference = Some("1")
  )

  val evse2 = Evse(
    EvseUid("BE-BEC-E041503002"),
    lastUpdated = dateOfUpdate,
    ConnectorStatus.Available,
    capabilities = List(Capability.Reservable),
    connectors = List(connector1),
    floorLevel = Some("-1"),
    physicalReference = Some("1")
  )

  val evse3 = Evse(
    EvseUid("BE-BEC-E041503003"),
    lastUpdated = dateOfUpdate,
    ConnectorStatus.Available,
    capabilities = List(Capability.Reservable),
    connectors = List(connector1),
    floorLevel = Some("-1"),
    physicalReference = Some("2")
  )

  val excp_open_begin = parseToUtc("2014-06-21T09:00:00+02:00")
  val excp_open_end = parseToUtc("2014-06-21T12:00:00+02:00")
  val excp_close_begin = parseToUtc("2014-06-24T00:00:00+02:00")
  val excp_close_end = parseToUtc("2014-06-25T00:00:00+02:00")

  val hours1 = Hours(
    regularHours = List(
      RegularHours(1, "08:00", "20:00"),
      RegularHours(2, "08:00", "20:00"),
      RegularHours(3, "08:00", "20:00"),
      RegularHours(4, "08:00", "20:00"),
      RegularHours(5, "08:00", "20:00")
    ),
    twentyfourseven = false,
    exceptionalOpenings = List(
      ExceptionalPeriod(excp_open_begin, excp_open_end)),
    exceptionalClosings = List(
      ExceptionalPeriod(excp_close_begin, excp_close_end)
    )
  )
  val dir1 = DisplayText("en", "left, left, left, right, left")
  val location1 = Location(
    LocationId("LOC1"),
    lastUpdated = dateOfUpdate,
    `type` = LocationType.OnStreet,
    Some("Gent Zuid"),
    address = "F.Rooseveltlaan 3A",
    city = "Gent",
    postalCode = "9000",
    country = CountryCode("BEL"),
    coordinates = GeoLocation("3.729945", "51.047594"),
    evses = List(evse1),
    directions = List(dir1),
    operator = None,
    suboperator = None,
    openingTimes = Some(hours1),
    relatedLocations = List.empty,
    chargingWhenClosed = Some(true),
    images = List.empty,
    energyMix = Some(EnergyMix(
      isGreenEnergy = true,
      energySources = Nil,
      environImpact = Nil,
      Some("Greenpeace Energy eG"),
      Some("eco-power")
    ))
  )

  val location2 = Location(
    LocationId("LOC2"),
    lastUpdated = dateOfUpdate,
    `type` = LocationType.OnStreet,
    Some("Gent Zuid"),
    address = "F.Rooseveltlaan 30",
    city = "Gent",
    postalCode = "9000",
    country = CountryCode("BEL"),
    coordinates = GeoLocation("3.729955", "51.047604"),
    evses = List(evse2,evse3),
    directions = List(dir1),
    operator = None,
    suboperator = None,
    openingTimes = None,
    relatedLocations = List.empty,
    chargingWhenClosed = Some(true),
    images = List.empty
  )

  val geoLocationJson1 =
    s"""
       |{
       |  "latitude": "3.729945",
       |  "longitude": "51.047594"
       |}
   """.stripMargin

  val geoLocationJson2 =
    s"""
       |{
       |  "latitude": "3.729955",
       |  "longitude": "51.047604"
       |}
   """.stripMargin

  val evseJson1 =
    s"""
       |    {
       |      "uid": "BE-BEC-E041503001",
       |      "last_updated": "2016-12-31T23:59:59Z",
       |      "status": "AVAILABLE",
       |      "status_schedule": [],
       |      "capabilities": [
       |        "RESERVABLE"
       |      ],
       |      "connectors": [
       |        {
       |          "id": "1",
       |          "last_updated": "2016-12-31T23:59:59Z",
       |          "standard": "IEC_62196_T2",
       |          "format": "CABLE",
       |          "power_type": "AC_3_PHASE",
       |          "voltage": 230,
       |          "amperage": 16,
       |          "tariff_id": "kwrate"
       |        },
       |        {
       |          "id": "2",
       |          "last_updated": "2016-12-31T23:59:59Z",
       |          "standard": "IEC_62196_T2",
       |          "format": "SOCKET",
       |          "power_type": "AC_3_PHASE",
       |          "voltage": 230,
       |          "amperage": 16,
       |          "tariff_id": "timerate"
       |        }
       |      ],
       |      "directions": [],
       |      "floor_level": "-1",
       |      "images": [],
       |      "parking_restrictions": [],
       |      "physical_reference": "1",
       |      "floor_level": "-1"
       |    }
 """.stripMargin

  val evseJson2 =
    s"""
       |    {
       |      "uid": "BE-BEC-E041503002",
       |      "last_updated": "2016-12-31T23:59:59Z",
       |      "status": "AVAILABLE",
       |      "status_schedule": [],
       |      "capabilities": [
       |        "RESERVABLE"
       |      ],
       |      "connectors": [
       |        {
       |          "id": "1",
       |          "last_updated": "2016-12-31T23:59:59Z",
       |          "standard": "IEC_62196_T2",
       |          "format": "CABLE",
       |          "power_type": "AC_3_PHASE",
       |          "voltage": 230,
       |          "amperage": 16,
       |          "tariff_id": "kwrate"
       |        }
       |      ],
       |      "directions": [],
       |      "floor_level": "-1",
       |      "images": [],
       |      "parking_restrictions": [],
       |      "physical_reference": "1",
       |      "floor_level": "-1"
       |    }
 """.stripMargin

  val evseJson3 =
    s"""
       |    {
       |      "uid": "BE-BEC-E041503003",
       |      "last_updated": "2016-12-31T23:59:59Z",
       |      "status": "AVAILABLE",
       |      "status_schedule": [],
       |      "capabilities": [
       |        "RESERVABLE"
       |      ],
       |      "connectors": [
       |        {
       |          "id": "1",
       |          "last_updated": "2016-12-31T23:59:59Z",
       |          "standard": "IEC_62196_T2",
       |          "format": "CABLE",
       |          "power_type": "AC_3_PHASE",
       |          "voltage": 230,
       |          "amperage": 16,
       |          "tariff_id": "kwrate"
       |        }
       |      ],
       |      "directions": [],
       |      "floor_level": "-1",
       |      "images": [],
       |      "parking_restrictions": [],
       |      "physical_reference": "2",
       |      "floor_level": "-1"
       |    }
 """.stripMargin

  val locationJson1 =
    s"""
       |    {
       |      "id": "LOC1",
       |      "last_updated": "2016-12-31T23:59:59Z",
       |      "type": "ON_STREET",
       |      "name": "Gent Zuid",
       |      "address": "F.Rooseveltlaan 3A",
       |      "city": "Gent",
       |      "postal_code": "9000",
       |      "country": "BEL",
       |      "coordinates": $geoLocationJson1,
       |      "related_locations": [],
       |      "evses": [$evseJson1],
       |      "directions": [{"language":"en","text":"left, left, left, right, left"}],
       |      "opening_times": {
       |        "twentyfourseven": false,
       |		    "regular_hours": [
       |		      {
       |		        "weekday": 1,
       |		        "period_begin": "08:00",
       |		        "period_end": "20:00"
       |		      },
       |		      {
       |		        "weekday": 2,
       |		        "period_begin": "08:00",
       |		        "period_end": "20:00"
       |		      },
       |		      {
       |		        "weekday": 3,
       |		        "period_begin": "08:00",
       |		        "period_end": "20:00"
       |		      },
       |		      {
       |		        "weekday": 4,
       |		        "period_begin": "08:00",
       |		        "period_end": "20:00"
       |		      },
       |		      {
       |		        "weekday": 5,
       |		        "period_begin": "08:00",
       |		        "period_end": "20:00"
       |		      }
       |		    ],
       |		    "exceptional_openings": [
       |		      {
       |		        "period_begin": "2014-06-21T07:00:00Z",
       |		        "period_end": "2014-06-21T10:00:00Z"
       |		      }
       |		    ],
       |		    "exceptional_closings": [
       |		      {
       |		        "period_begin": "2014-06-23T22:00:00Z",
       |		        "period_end": "2014-06-24T22:00:00Z"
       |		      }
       |		    ]
       |		  },
       |      "facilities": [],
       |      "images":[],
       |      "charging_when_closed": true,
       |      "energy_mix": {
       |        "is_green_energy": true,
       |        "energy_sources": [],
       |        "environ_impact": [],
       |        "supplier_name": "Greenpeace Energy eG",
       |        "energy_product_name": "eco-power"
       |      }
       |    }
 """.stripMargin

  val locationJson2 =
    s"""
       |    {
       |      "id": "LOC2",
       |      "last_updated": "2016-12-31T23:59:59Z",
       |      "type": "ON_STREET",
       |      "name": "Gent Zuid",
       |      "address": "F.Rooseveltlaan 30",
       |      "city": "Gent",
       |      "postal_code": "9000",
       |      "country": "BEL",
       |      "coordinates": $geoLocationJson2,
       |      "related_locations": [],
       |      "evses": [$evseJson2, $evseJson3],
       |      "directions": [{"language":"en","text":"left, left, left, right, left"}],
       |      "facilities": [],
       |      "images":[],
       |      "charging_when_closed": true
       |    }
 """.stripMargin

  val operatorJsonWithNulls1 =
    s"""
       |{
       |  "identifier": null,
       |  "phone": "+31253621489",
       |  "url": null
       |}
    """.stripMargin

  val operatorJsonNoNulls1 =
    s"""
       |{
       |  "phone": "+31253621489"
       |}
    """.stripMargin

}
