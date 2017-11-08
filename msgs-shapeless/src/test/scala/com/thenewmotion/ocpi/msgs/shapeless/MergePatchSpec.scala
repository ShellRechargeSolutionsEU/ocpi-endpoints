package com.thenewmotion.ocpi.msgs.shapeless

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.AuthMethod
import com.thenewmotion.ocpi.msgs.{CountryCode, CurrencyCode}
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionId, SessionPatch, SessionStatus}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import org.specs2.mutable.Specification
import mergeSyntax._
import org.specs2.specification.Scope

class MergePatchSpec extends Specification {

  "MergePatch" should {
    "merge a patch into a token" in new TokenScope {
      val patch = TokenPatch(valid = Some(false))

      token.merge(patch) mustEqual
        Token(
          uid = TokenUid("23455655A"),
          `type` = TokenType.Rfid,
          authId = AuthId("NL-TNM-000660755-V"),
          visualNumber = Some("NL-TNM-066075-5"),
          issuer = "TheNewMotion",
          valid = false,
          whitelist = WhitelistType.Allowed,
          lastUpdated = ZonedDateTime.parse("2017-01-24T10:00:00.000Z")
        )
    }

    "merge a patch into a session" in new SessionScope {
      val patch = SessionPatch(currency = Some(CurrencyCode("GBP")))

      session1.merge(patch) mustEqual
        session1.copy(
          currency = CurrencyCode("GBP")
        )
    }
  }

  trait TokenScope extends Scope {
    val token = Token(
      uid = TokenUid("23455655A"),
      `type` = TokenType.Rfid,
      authId = AuthId("NL-TNM-000660755-V"),
      visualNumber = Some("NL-TNM-066075-5"),
      issuer = "TheNewMotion",
      valid = true,
      whitelist = WhitelistType.Allowed,
      lastUpdated = ZonedDateTime.parse("2017-01-24T10:00:00.000Z")
    )
  }

  trait SessionScope extends Scope {

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
  }

}
