package com.thenewmotion.ocpi.common

import java.time.ZonedDateTime

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class PaginatedRouteSpec extends Specification with Specs2RouteTest {

  "PaginatedRoute" should {

    "have a default limit" in new TestScope {
      val testRoute =
        get {
          pathEndOrSingleSlash {
            paged { (pager: Pager, _: Option[ZonedDateTime], _: Option[ZonedDateTime]) =>
              complete(pager.limit.toString)
            }
          }
        }

      Get() ~> testRoute ~> check {
        responseAs[String] shouldEqual "5"
      }
    }

    "have a max limit" in new TestScope {
      val testRoute =
        get {
          pathEndOrSingleSlash {
            paged { (pager: Pager, _: Option[ZonedDateTime], _: Option[ZonedDateTime]) =>
              complete(pager.limit.toString)
            }
          }
        }

      Get("?limit=100") ~> testRoute ~> check {
        responseAs[String] shouldEqual "10"
      }
    }

    "respond with X-Limit and X-Total-Count headers when there is no next page" in new TestScope {
      val testRoute =
        get {
          pathEndOrSingleSlash {
            paged { (pager: Pager, _: Option[ZonedDateTime], _: Option[ZonedDateTime]) =>
              respondWithPaginationHeaders(pager, PaginatedResult(List("1", "2", "3"), 3)) {
                complete("OK")
              }
            }
          }
        }

      Get("?limit=3") ~> testRoute ~> check {
        response.header[Link] must beNone
        header("X-Limit") must beSome(RawHeader("X-Limit", "3"))
        header("X-Total-Count") must beSome(RawHeader("X-Total-Count", "3"))
      }
    }

    "respond with X-Limit, X-Total-Count and Link headers where there is another page" in new TestScope {
      val testRoute =
        get {
          pathEndOrSingleSlash {
            paged { (pager: Pager, _: Option[ZonedDateTime], _: Option[ZonedDateTime]) =>
              respondWithPaginationHeaders(pager, PaginatedResult(List("1", "2", "3"), 100)) {
                 complete("OK")
              }
            }
          }
        }

      Get("?limit=3") ~> testRoute ~> check {
        response.header[Link].map(_.values) must beLike {
          case Some(List(lv)) =>
            lv.uri.query().get("offset") must beSome("3")
            lv.uri.query().get("limit") must beSome("3")
            lv.params mustEqual List(LinkParams.next)
        }
        header("X-Limit") must beSome(RawHeader("X-Limit", "3"))
        header("X-Total-Count") must beSome(RawHeader("X-Total-Count", "100"))
      }
    }

    "pass through extra query parameters to next link" in new TestScope {
      val testRoute =
        get {
          pathEndOrSingleSlash {
            paged { (pager: Pager, _: Option[ZonedDateTime], _: Option[ZonedDateTime]) =>
              respondWithPaginationHeaders(pager, PaginatedResult((1 to 5).map(_.toString), 100)) {
                complete("OK")
              }
            }
          }
        }

      Get("?horses=neigh&sheep=baa") ~> testRoute ~> check {
        response.header[Link].map(_.values) must beLike {
          case Some(List(lv)) =>
            lv.uri.query().get("horses") must beSome("neigh")
            lv.uri.query().get("sheep") must beSome("baa")
            lv.uri.query().get("offset") must beSome("5")
            lv.uri.query().get("limit") must beSome("5")
            lv.params mustEqual List(LinkParams.next)
        }
      }
    }
  }

  trait TestScope extends Scope with PaginatedRoute {
    override def DefaultLimit = 5
    override def MaxLimit = 10
  }
}
