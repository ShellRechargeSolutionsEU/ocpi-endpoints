package com.thenewmotion.ocpi.cdrs

import java.time.ZonedDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.{Link, LinkParams, RawHeader}
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.common.{OcpiDirectives, Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs._
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.{CountryCode, CurrencyCode, GlobalPartyId}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future

class CpoCdrsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "CPO Cdrs Route" should {
    "return paginated list of cdrs with headers as per OCPI specs" in new TestScope {

      val InitialClientOffset = 0
      val ClientPageLimit = 100

      val ResultingPageLimit = Math.min(ClientPageLimit, ServerPageLimit)
      val ServerTotal = 200

      val SecondPageOffset = InitialClientOffset + ResultingPageLimit
      val ThirdPageOffset = SecondPageOffset + ResultingPageLimit

      service.cdrs(apiUser, Pager(InitialClientOffset, ResultingPageLimit), dateFromP, dateToP) returns
        Future(Right(PaginatedResult(List(cdr), ServerTotal)))

      Get(s"/?offset=$InitialClientOffset&limit=$ClientPageLimit&date_from=$dateFrom&date_to=$dateTo") ~> route.routeWithoutRh(apiUser) ~> check {
        response.header[Link] must beSome(
          Link(Uri(s"http://example.com/?offset=$SecondPageOffset&limit=$ResultingPageLimit&date_from=$dateFrom&date_to=$dateTo"), LinkParams.next))
        headers.find(_.name == "X-Limit") must beSome(RawHeader("X-Limit", ResultingPageLimit.toString))
        headers.find(_.name == "X-Total-Count") must beSome(RawHeader("X-Total-Count", ServerTotal.toString))
        there was one(service).cdrs(apiUser, Pager(InitialClientOffset, ResultingPageLimit), dateFromP, dateToP)
      }


      service.cdrs(apiUser, Pager(SecondPageOffset, ResultingPageLimit), dateFromP, dateToP) returns
        Future(Right(PaginatedResult(List(cdr), ServerTotal)))

      Get(s"/?offset=$SecondPageOffset&limit=$ResultingPageLimit&date_from=$dateFrom&date_to=$dateTo") ~> route.routeWithoutRh(apiUser) ~> check {
        response.header[Link] must beSome(
          Link(Uri(s"http://example.com/?offset=$ThirdPageOffset&limit=$ResultingPageLimit&date_from=$dateFrom&date_to=$dateTo"), LinkParams.next))
        headers.find(_.name == "X-Limit") must beSome(RawHeader("X-Limit", "50"))
        headers.find(_.name == "X-Total-Count") must beSome(RawHeader("X-Total-Count", "200"))
        there was one(service).cdrs(apiUser, Pager(SecondPageOffset, ResultingPageLimit), dateFromP, dateToP)
      }
    }
  }

  trait TestScope extends Scope with SprayJsonSupport with OcpiDirectives {
    val ServerPageLimit = 50

    val apiUser = GlobalPartyId("NL", "TNM")

    val dateFrom = "2019-12-17T10:17:07Z"
    val dateTo = "2019-12-17T11:09:02Z"

    val dateFromP = Some(ZonedDateTime.parse(dateFrom))
    val dateToP = Some(ZonedDateTime.parse(dateTo))

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
        GeoLocation(Latitude("3.72994"), Longitude("51.04759")),
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

    val service = mock[CpoCdrsService]

    val route = CpoCdrsRoute(service, ServerPageLimit, ServerPageLimit)
  }
}
