package com.thenewmotion.ocpi.commands

import java.util.UUID
import akka.http.scaladsl.testkit.Specs2RouteTest
import cats.effect.IO
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.GenericSuccess
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{CommandResponse, CommandResponseType}
import com.thenewmotion.ocpi.msgs.{GlobalPartyId, SuccessResp}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CommandResponseRouteSpec extends Specification with Specs2RouteTest {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

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

    import com.thenewmotion.ocpi.common.HktMarshallableFromECInstances._
    val route = new CommandResponseRoute[IO]( (gpi, uuid, crt) =>
        IO.pure(Some(SuccessResp(GenericSuccess)))
    )
  }
}
