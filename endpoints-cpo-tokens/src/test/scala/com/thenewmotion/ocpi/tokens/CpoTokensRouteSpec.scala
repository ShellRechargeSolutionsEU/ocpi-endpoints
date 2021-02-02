package com.thenewmotion.ocpi
package tokens

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import akka.http.scaladsl.testkit.Specs2RouteTest
import cats.syntax.either._
import cats.{Applicative, Id}
import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import com.thenewmotion.ocpi.msgs.{ErrorResp, GlobalPartyId, Language}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CpoTokensRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import TokenError.TokenNotFound
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.model.StatusCodes
  import msgs.sprayjson.v2_1.protocol._
  import msgs.v2_1.Tokens._

  "tokens endpoint" should {
    "reject unauthorized access" in new TokensTestScope {
      val unAuthorizedUser = GlobalPartyId("NL", "SBM")

      Put(s"$tokenPath/$tokenUid") ~> akka.http.scaladsl.server.Route.seal(
        cpoTokensRoute(unAuthorizedUser)) ~> check {
        responseAs[ErrorResp]
        status mustEqual StatusCodes.Forbidden
      }
    }

    "accept a new token object" in new TokensTestScope {
      val token = Token(
        tokenUid,
        TokenType.Rfid,
        authId = AuthId("FA54320"),
        visualNumber = Some("DF000-2001-8999"),
        issuer = "TheNewMotion",
        valid = true,
        WhitelistType.Allowed,
        language = Some(Language("nl")),
        lastUpdated = ZonedDateTime.now
      )

      cpoTokensService.createOrUpdateToken(
        ===(apiUser),
        ===(tokenUid),
        any[Token]
      )(any[Applicative[Id]]) returns CreateOrUpdateResult.Created.asRight

      def beMostlyEqualTo = (be_==(_: Token)) ^^^ ((_: Token).copy(lastUpdated =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC)))

      Put(s"$tokenPath/$tokenUid", token) ~>
        cpoTokensRoute(apiUser) ~> check {
        there was one(cpoTokensService).createOrUpdateToken(
          ===(apiUser),
          ===(tokenUid),
          beMostlyEqualTo(token)
        )(any[Applicative[Id]])
        there were noCallsTo(cpoTokensService)
      }
    }

    "accept patches to a token object" in new TokensTestScope {
      val whitelistPatch = Some(WhitelistType.Always)
      val tokenPatch = TokenPatch(
        whitelist = whitelistPatch
      )

      cpoTokensService.updateToken(
        apiUser,
        tokenUid,
        tokenPatch
      ) returns ().asRight

      Patch(s"$tokenPath/$tokenUid", tokenPatch) ~>
        cpoTokensRoute(apiUser) ~> check {
        there was one(cpoTokensService).updateToken(
          apiUser,
          tokenUid,
          tokenPatch
        )
        there were noCallsTo(cpoTokensService)
      }
    }

    "retrieve a token object" in new TokensTestScope {
      cpoTokensService.token(
        apiUser,
        tokenUid
      ) returns TokenNotFound().asLeft

      Get(s"$tokenPath/$tokenUid") ~>
        cpoTokensRoute(apiUser) ~> check {
        there was one(cpoTokensService).token(
          apiUser,
          tokenUid
        )
        there were noCallsTo(cpoTokensService)
      }
    }
  }

  trait TokensTestScope extends Scope {
    val tokenUid = TokenUid("012345678")
    val countryCodeString = "NL"
    val operatorIdString = "TNM"
    val apiUser = GlobalPartyId(countryCodeString, operatorIdString)
    val tokenPath = s"/$countryCodeString/$operatorIdString"
    val cpoTokensService = mock[CpoTokensService[Id]]
    import com.thenewmotion.ocpi.common.HktMarshallableFromECInstances.idMarshaller
    val cpoTokensRoute = CpoTokensRoute(cpoTokensService)
  }
}
