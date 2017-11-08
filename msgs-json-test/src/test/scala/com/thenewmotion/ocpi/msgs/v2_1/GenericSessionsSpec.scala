package com.thenewmotion.ocpi.msgs.v2_1

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import com.thenewmotion.ocpi.msgs.{CountryCode, CurrencyCode}
import Cdrs.AuthMethod
import Locations._
import Sessions.{Session, SessionId, SessionPatch, SessionStatus}
import Tokens.AuthId
import org.specs2.specification.core.Fragments

import scala.language.higherKinds

trait GenericSessionsSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit sessionR: GenericJsonReader[Session],
    sessionW: GenericJsonWriter[Session],
    sessionPatchR: GenericJsonReader[SessionPatch],
    sessionPatchW: GenericJsonWriter[SessionPatch]
  ): Fragments = {
    "Session" should {
      testPair(session1, parse(sessionJson1))
    }

    "Session Patch" should {
      testPair(sessionPatch1, parse(sessionPatchJson1))
    }
  }

  private def parseToUtc(s: String) =
    ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC)

  private val dateOfUpdate = parseToUtc("2016-12-31T23:59:59Z")

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

  val evse1 = Evse(
    EvseUid("BE-BEC-E041503001"),
    lastUpdated = dateOfUpdate,
    ConnectorStatus.Available,
    capabilities = List(Capability.Reservable),
    connectors = List(connector1),
    floorLevel = Some("-1"),
    physicalReference = Some("1")
  )

  val location1 = Location(
    LocationId("LOC1"),
    lastUpdated = dateOfUpdate,
    `type` = LocationType.OnStreet,
    Some("Gent Zuid"),
    address = "F.Rooseveltlaan 3A",
    city = "Gent",
    postalCode = "9000",
    country = CountryCode("BEL"),
    coordinates = GeoLocation(Latitude("3.729945"), Longitude("51.047594")),
    evses = List(evse1),
    directions = List.empty,
    operator = None,
    suboperator = None,
    openingTimes = None,
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

  val session1 = Session(
    id = SessionId("abc"),
    startDatetime = parseToUtc("2017-03-01T08:00:00Z"),
    endDatetime = Some(parseToUtc("2017-03-01T10:00:00Z")),
    kwh = 1000,
    authId = AuthId("ABC1234"),
    authMethod = AuthMethod.AuthRequest,
    location = location1,
    meterId = None,
    currency = CurrencyCode("EUR"),
    chargingPeriods = Nil,
    totalCost = Some(10.24),
    status = SessionStatus.Completed,
    lastUpdated = dateOfUpdate
  )

  val sessionJson1 =
    """
      |{
      |    "auth_id": "ABC1234",
      |    "auth_method": "AUTH_REQUEST",
      |    "currency": "EUR",
      |    "end_datetime": "2017-03-01T10:00:00Z",
      |    "id": "abc",
      |    "kwh": 1000,
      |    "last_updated": "2016-12-31T23:59:59Z",
      |	   "charging_periods": [],
      |    "location": {
      |        "address": "F.Rooseveltlaan 3A",
      |        "charging_when_closed": true,
      |        "city": "Gent",
      |        "coordinates": {
      |            "latitude": "3.729945",
      |            "longitude": "51.047594"
      |        },
      |        "country": "BEL",
      |        "directions": [],
      |        "energy_mix": {
      |            "energy_product_name": "eco-power",
      |            "energy_sources": [],
      |            "environ_impact": [],
      |            "is_green_energy": true,
      |            "supplier_name": "Greenpeace Energy eG"
      |        },
      |        "evses": [
      |            {
      |                "capabilities": [
      |                    "RESERVABLE"
      |                ],
      |                "connectors": [
      |                    {
      |                        "amperage": 16,
      |                        "format": "CABLE",
      |                        "id": "1",
      |                        "last_updated": "2016-12-31T23:59:59Z",
      |                        "power_type": "AC_3_PHASE",
      |                        "standard": "IEC_62196_T2",
      |                        "tariff_id": "kwrate",
      |                        "voltage": 230
      |                    }
      |                ],
      |                "directions": [],
      |                "floor_level": "-1",
      |                "images": [],
      |                "last_updated": "2016-12-31T23:59:59Z",
      |                "parking_restrictions": [],
      |                "physical_reference": "1",
      |                "status": "AVAILABLE",
      |                "status_schedule": [],
      |                "uid": "BE-BEC-E041503001"
      |            }
      |        ],
      |        "facilities": [],
      |        "id": "LOC1",
      |        "images": [],
      |        "last_updated": "2016-12-31T23:59:59Z",
      |        "name": "Gent Zuid",
      |        "postal_code": "9000",
      |        "related_locations": [],
      |        "type": "ON_STREET"
      |    },
      |    "start_datetime": "2017-03-01T08:00:00Z",
      |    "status": "COMPLETED",
      |    "total_cost": 10.24
      |}
    """.stripMargin

  val sessionPatch1 = SessionPatch(totalCost = Some(9.5))

  val sessionPatchJson1 =
    """
      |{
      |    "total_cost": 9.5
      |}""".stripMargin

}
