package com.thenewmotion.ocpi.commands

import java.net.UnknownHostException
import java.time.ZonedDateTime
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import cats.effect.IO
import com.thenewmotion.ocpi.common.IOMatchersExt
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._
import com.thenewmotion.ocpi.msgs.v2_1.Commands
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{CommandName, CommandResponseType}
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{EvseUid, LocationId}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import com.thenewmotion.ocpi.msgs.{AuthToken, ErrorResp, Url}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class CommandClientSpec(implicit ec: ExecutionContext) extends Specification with IOMatchersExt {

  "MSP Command client" should {

    val cmd = Commands.Command.StartSession(
      Url("http://localhost:8096"),
      Token(
        TokenUid("ABC12345678"),
        TokenType.Rfid,
        AuthId("NL-TNM-BLAH"),
        visualNumber = None,
        "TheNewMotion",
        valid = true,
        WhitelistType.Always,
        language = None,
        lastUpdated = ZonedDateTime.now
      ),
      LocationId("1234"),
      Some(EvseUid("1234_A"))
    )

    "Start a session using deprecated 2.1.1" in new TestScope {
      client.sendCommand(commandUrl, AuthToken[Ours]("auth"), cmd) must returnValueLike[Either[ErrorResp, CommandResponseType]] {
        case Right(r) =>
          r === CommandResponseType.Accepted
      }
    }

    "Start a session using new 2.1.1-d2 interpretation" in new TestScope {
      clientD2.sendCommand(commandUrl, AuthToken[Ours]("auth"), cmd) must returnValueLike[Either[ErrorResp, CommandResponseType]] {
        case Right(r) =>
          r === CommandResponseType.Accepted
      }
    }

    "handle trailing slashes in command module URLs" in new TestScope {

      private val pathWithTrailingSlash = commandUrl.path ++ Uri.Path.SingleSlash

      client.sendCommand(commandUrl.withPath(pathWithTrailingSlash), AuthToken[Ours]("auth"), cmd) must returnValueLike[Either[ErrorResp, CommandResponseType]] {
        case Right(r) =>
          r === CommandResponseType.Accepted
      }
    }
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()

    implicit val http = Http()

    val commandUrl = Uri("http://localhost:8095/cpo/2.1.1/commands")

    def successResp = HttpResponse(
      StatusCodes.OK, entity = HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 1000,
           |  "timestamp": "2010-01-01T00:00:00Z",
           |  "data": "ACCEPTED"
           |}
           |""".stripMargin.getBytes)
    )

    def successRespD2 = HttpResponse(
      StatusCodes.OK, entity = HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 1000,
           |  "timestamp": "2010-01-01T00:00:00Z",
           |  "data": {
           |    "result": "ACCEPTED"
           |    }
           |}
           |""".stripMargin.getBytes)
    )

    implicit val timeout: Timeout = Timeout(FiniteDuration(20, "seconds"))

    val startCommandUrl = s"$commandUrl/${CommandName.StartSession}"

    def requestWithAuth(uri: String) = uri match {
      case `startCommandUrl` => IO.pure(successResp)
      case x                 => IO.raiseError(new UnknownHostException(x.toString))
    }

    def requestWithAuthD2(uri: String) = uri match {
      case `startCommandUrl` => IO.pure(successRespD2)
      case x                 => IO.raiseError(new UnknownHostException(x.toString))
    }

    lazy val client = new TestMspCommandsClient(requestWithAuth)
    lazy val clientD2 = new TestMspCommandsClient(requestWithAuthD2)
  }
}

object GenericRespTypes {
  case class TestData(id: String)

  implicit val testDataFormat = jsonFormat1(TestData)
}


class TestMspCommandsClient(reqWithAuthFunc: String => IO[HttpResponse])
  (implicit httpExt: HttpExt) extends CommandClient {

  override def requestWithAuth(http: HttpExt, req: HttpRequest, token: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x).unsafeToFuture() }

}
