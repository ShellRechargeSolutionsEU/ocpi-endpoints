package com.thenewmotion.ocpi
package tokens

import common._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{PathMatcher1, Route}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import msgs._
import msgs.OcpiStatusCode.GenericClientFailure
import msgs.OcpiStatusCode.GenericSuccess
import msgs.v2_1.Tokens._
import tokens.TokenError._

import scala.concurrent.ExecutionContext

class CpoTokensRoute(
  service: CpoTokensService
)(
  implicit successTokenM: SuccessRespMar[Token],
  successUnitM: SuccessRespMar[Unit],
  errorM: ErrRespMar,
  tokenU: FromEntityUnmarshaller[Token],
  tokenPU: FromEntityUnmarshaller[TokenPatch]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

  implicit def tokenErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[TokenError] = {
    em.compose[TokenError] { tokenError =>
      val statusCode = tokenError match {
        case _: TokenNotFound => NotFound
        case (_: TokenCreationFailed | _: TokenUpdateFailed) => OK
        case _ => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, tokenError.reason)
    }
  }

  private val TokenUidSegment: PathMatcher1[TokenUid] = Segment.map(TokenUid(_))

  def route(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ): Route =
    handleRejections(OcpiRejectionHandler.Default) {
      (authPathPrefixGlobalPartyIdEquality(apiUser) & pathPrefix(TokenUidSegment)) { tokenUid =>
        pathEndOrSingleSlash {
          put {
            entity(as[Token]) { token =>
              complete {
                service.createOrUpdateToken(apiUser, tokenUid, token).mapRight { created =>
                  (if (created) Created else OK, SuccessResp(GenericSuccess))
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
