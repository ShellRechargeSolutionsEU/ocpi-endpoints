package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

import com.thenewmotion.ocpi.msgs.{CountryCode, CurrencyCode}
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.AuthMethod
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionId, SessionStatus}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.AuthId
import org.specs2.mutable.Specification

class SessionsSpec extends Specification {

  "Session" should {
    "Accept Locations with exactly one Evse and one Connector" >> {
      val loc = location(evse(connector))
      session1(loc) must not(throwA[Exception])
    }

    "Error for Locations with more than one evse" >> {
      val loc = location(evse(connector), evse(connector))
      session1(loc) must throwA[IllegalArgumentException]
    }

    "Error for Locations with more than one connector" >> {
      val loc = location(evse(connector, connector))
      session1(loc) must throwA[IllegalArgumentException]
    }
  }

  private def parseToUtc(s: String) =
    ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC)

  private val dateOfUpdate = parseToUtc("2016-12-31T23:59:59Z")

  val connector = Connector(
    ConnectorId("1"),
    lastUpdated = dateOfUpdate,
    ConnectorType.`IEC_62196_T2`,
    ConnectorFormat.Cable,
    PowerType.AC3Phase,
    230,
    16,
    tariffId = Some("kwrate")
  )

  def evse(connectors: Connector*) = Evse(
    EvseUid("BE-BEC-E041503001"),
    lastUpdated = dateOfUpdate,
    ConnectorStatus.Available,
    capabilities = List(Capability.Reservable),
    connectors = connectors,
    floorLevel = Some("-1"),
    physicalReference = Some("1")
  )

  def location(evses: Evse*) = Location(
    LocationId("LOC1"),
    lastUpdated = dateOfUpdate,
    `type` = LocationType.OnStreet,
    Some("Gent Zuid"),
    address = "F.Rooseveltlaan 3A",
    city = "Gent",
    postalCode = "9000",
    country = CountryCode("BEL"),
    coordinates = GeoLocation(Latitude("3.729945"), Longitude("51.047594")),
    evses = evses,
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

  def session1(location: Location) = Session(
    id = SessionId("abc"),
    startDatetime = parseToUtc("2017-03-01T08:00:00Z"),
    endDatetime = Some(parseToUtc("2017-03-01T10:00:00Z")),
    kwh = 1000,
    authId = AuthId("ABC1234"),
    authMethod = AuthMethod.AuthRequest,
    location = location,
    meterId = None,
    currency = CurrencyCode("EUR"),
    chargingPeriods = Nil,
    totalCost = Some(10.24),
    status = SessionStatus.Completed,
    lastUpdated = dateOfUpdate
  )

}
