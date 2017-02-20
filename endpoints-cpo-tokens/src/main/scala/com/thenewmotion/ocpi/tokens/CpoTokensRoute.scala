package com.thenewmotion.ocpi
package tokens

import common.{DisjunctionMarshalling, OcpiDirectives, OcpiRejectionHandler}
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import msgs._
import msgs.v2_1.OcpiJsonProtocol._
import msgs.OcpiStatusCode.GenericClientFailure
import msgs.OcpiStatusCode.GenericSuccess
import msgs.v2_1.Tokens._
import tokens.TokenError._
import scala.concurrent.ExecutionContext

class CpoTokensRoute(
  service: CpoTokensService
) extends JsonApi with DisjunctionMarshalling with OcpiDirectives {

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

  def route(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext) =
    handleRejections(OcpiRejectionHandler.Default) {
      (authPathPrefixGlobalPartyIdEquality(apiUser) & pathPrefix(Segment)) { tokenUid =>
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
                SuccessWithDataResp(GenericSuccess, None, data = token)
              }
            }
          }
        }
      }
    }
}
