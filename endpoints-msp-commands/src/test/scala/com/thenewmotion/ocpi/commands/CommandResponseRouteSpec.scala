package com.thenewmotion.ocpi.commands

import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.msgs.{GlobalPartyId, SuccessResp}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import java.util.UUID

import com.thenewmotion.ocpi.msgs.OcpiStatusCode.GenericSuccess
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{CommandResponse, CommandResponseType}

import scala.concurrent.Future

class CommandResponseRouteSpec extends Specification with Specs2RouteTest {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.CommandsJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._

  "command response endpoint" should {

    "receive a response for a valid command" in new TestScope {
      val uuid = UUID.fromString("fd1ff5e3-3ca1-4855-8ad2-c077dba0c67c")

      val body = CommandResponse(CommandResponseType.Accepted)

      Post(s"/$uuid", body) ~> route.routeWithoutRh(apiUser) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
      }
    }
  }

  trait TestScope extends Scope {

    val apiUser = GlobalPartyId("NL", "TNM")

    val route = new CommandResponseRoute( (gpi, uuid, crt) =>
        Future.successful(Some(SuccessResp(GenericSuccess)))
    )
  }
}
