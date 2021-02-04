package com.thenewmotion.ocpi.example

import java.time.ZonedDateTime
import _root_.akka.actor.ActorSystem
import _root_.akka.http.scaladsl.Http
import _root_.akka.http.scaladsl.server.Directives._
import cats.effect.{ExitCode, IO, IOApp}
import com.thenewmotion.ocpi.common.{Pager, PaginatedResult, TokenAuthenticator}
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import com.thenewmotion.ocpi.tokens.{AuthorizeError, MspTokensRoute, MspTokensService}

/**
  * Uses cats-effect's IO datatype
  */
object ExampleCatsIO extends IOApp {

  object IOBasedTokensService extends MspTokensService[IO] {
    def tokens(
      globalPartyId: GlobalPartyId,
      pager: Pager,
      dateFrom: Option[ZonedDateTime] = None,
      dateTo: Option[ZonedDateTime] = None
    ): IO[PaginatedResult[Token]] = IO.pure(PaginatedResult(Nil, 0))

    def authorize(
      globalPartyId: GlobalPartyId,
      tokenUid: TokenUid,
      locationReferences: Option[LocationReferences]
    ): IO[Either[AuthorizeError, AuthorizationInfo]] = IO.pure(Right(AuthorizationInfo(Allowed.Allowed)))
  }

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher

  val service = IOBasedTokensService

  import com.thenewmotion.ocpi.common.HktMarshallableInstances._
  import com.thenewmotion.ocpi.msgs.circe.v2_1.CommonJsonProtocol._
  import com.thenewmotion.ocpi.msgs.circe.v2_1.TokensJsonProtocol._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  val tokensRoute = MspTokensRoute(service)

  val auth = new TokenAuthenticator[IO](_ => IO.pure(Some(GlobalPartyId("NL", "TNM"))))

  val topLevelRoute = {
    pathPrefix("example") {
      authenticateOrRejectWithChallenge(auth) { user =>
        pathPrefix("versions") {
          tokensRoute(user)
        }
      }
    }
  }



  override def run(args: List[String]): IO[ExitCode] =
    IO.fromFuture(IO(Http().newServerAt("localhost", 8080).bindFlow(topLevelRoute))).map(_ => ExitCode.Success)

}
