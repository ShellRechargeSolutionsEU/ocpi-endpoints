package com.thenewmotion.ocpi.cdrs

import java.time.ZonedDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Link
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.cdrs.CdrsError._
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs._
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.{CountryCode, CurrencyCode, GlobalPartyId, SuccessResp}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import cats.syntax.either._
import com.thenewmotion.ocpi.common.OcpiDirectives
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

import scala.concurrent.Future

class MspCdrsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "MspCdrsRoute" should {
    "return an existing Cdr" in new TestScope {
      service.cdr(apiUser, cdr.id) returns Future(cdr.asRight)
      Get("/NL/TNM/12345") ~> route.route(apiUser) ~> check {
        header[Link] must beNone
        there was one(service).cdr(apiUser, cdr.id)
        val res = entityAs[SuccessResp[Cdr]]
        res.data mustEqual cdr
      }
    }

    "handle NotFound failure" in new TestScope {
      service.cdr(apiUser, CdrId("does-not-exist")) returns Future(CdrNotFound().asLeft)

      Get("/NL/TNM/does-not-exist") ~> route.route(apiUser) ~> check {
        there was one(service).cdr(apiUser, CdrId("does-not-exist"))
        status mustEqual StatusCodes.NotFound
      }
    }

    "allow posting new cdr" in new TestScope {
      service.createCdr(apiUser, cdr) returns Future(().asRight)

      Post("/NL/TNM", cdr) ~> route.route(apiUser) ~> check {
        there was one(service).createCdr(apiUser, cdr)
        status mustEqual StatusCodes.Created
      }
    }

    "not allow updating cdr" in new TestScope {
      service.createCdr(apiUser, cdr) returns Future(CdrCreationFailed().asLeft)

      Post("/NL/TNM", cdr) ~> route.route(apiUser) ~> check {
        there was one(service).createCdr(apiUser, cdr)
        status mustEqual StatusCodes.OK
      }
    }
  }

  trait TestScope extends Scope with SprayJsonSupport with OcpiDirectives {
    val apiUser = GlobalPartyId("NL", "TNM")

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
      tariffs = None,
      chargingPeriods = List(
        ChargingPeriod(ZonedDateTime.parse("2015-06-29T21:39:09Z"), List(CdrDimension(CdrDimensionType.Time, 1.973)))
      ),
      totalCost = BigDecimal("4.00"),
      totalEnergy = 15.342,
      totalTime = 1.973,
      lastUpdated = ZonedDateTime.parse("2015-06-29T22:01:13Z")
    )

    val service = mock[MspCdrsService]

    val route = new MspCdrsRoute(service)
  }
}
