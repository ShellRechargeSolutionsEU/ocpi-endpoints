package com.thenewmotion.ocpi
package tokens

import _root_.akka.http.scaladsl.marshalling.ToResponseMarshaller
import _root_.akka.http.scaladsl.model.StatusCode
import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.{PathMatcher1, Route}
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.Applicative
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.{GenericClientFailure, GenericSuccess}
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import com.thenewmotion.ocpi.tokens.TokenError._

object CpoTokensRoute {
  def apply[F[_]: Applicative: HktMarshallable](
    service: CpoTokensService[F]
  )(
    implicit successTokenM: SuccessRespMar[Token],
    successUnitM: SuccessRespMar[Unit],
    errorM: ErrRespMar,
    tokenU: FromEntityUnmarshaller[Token],
    tokenPU: FromEntityUnmarshaller[TokenPatch]
  ): CpoTokensRoute[F] = new CpoTokensRoute(service)
}

class CpoTokensRoute[F[_]: Applicative: HktMarshallable] private[ocpi](
  service: CpoTokensService[F]
)(
  implicit successTokenM: SuccessRespMar[Token],
  successUnitM: SuccessRespMar[Unit],
  errorM: ErrRespMar,
  tokenU: FromEntityUnmarshaller[Token],
  tokenPU: FromEntityUnmarshaller[TokenPatch]
) extends EitherUnmarshalling with OcpiDirectives {

  implicit def tokenErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[TokenError] = {
    em.compose[TokenError] { tokenError =>
      val statusCode = tokenError match {
        case _: TokenNotFound                              => NotFound
        case _: TokenCreationFailed | _: TokenUpdateFailed => OK
        case _                                             => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, tokenError.reason)
    }
  }

  private val TokenUidSegment: PathMatcher1[TokenUid] = Segment.map(TokenUid(_))

  import HktMarshallableSyntax._

  def apply(
    apiUser: GlobalPartyId
  ): Route =
    handleRejections(OcpiRejectionHandler.Default) {
      (authPathPrefixGlobalPartyIdEquality(apiUser) & pathPrefix(TokenUidSegment)) { tokenUid =>
        pathEndOrSingleSlash {
          put {
            entity(as[Token]) { token =>
              complete {
                service.createOrUpdateToken(apiUser, tokenUid, token).mapRight { x =>
                  (x.httpStatusCode, SuccessResp(GenericSuccess))
                }
              }
            }
          } ~
          patch {
            entity(as[TokenPatch]) { patch =>
              complete {
                service.updateToken(apiUser, tokenUid, patch).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                }
              }
            }
          } ~
          get {
            complete {
              service.token(apiUser, tokenUid).mapRight { token =>
                SuccessResp(GenericSuccess, data = token)
              }
            }
          }
        }
      }
    }
}
