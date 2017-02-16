package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import common.{DisjunctionMarshalling, Pager, PaginatedRoute}
import msgs.{ErrorResp, GlobalPartyId, SuccessWithDataResp}
import org.joda.time.DateTime
import msgs.OcpiStatusCode._
import msgs.v2_1.Tokens.LocationReferences
import tokens.AuthorizeError._
import scala.concurrent.ExecutionContext

class MspTokensRoute(
  service: MspTokensService,
  val DefaultLimit: Int = 1000,
  val MaxLimit: Int = 1000
) extends JsonApi with PaginatedRoute with DisjunctionMarshalling {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  implicit def locationsErrorResp(implicit errorMarshaller: ToResponseMarshaller[(StatusCode, ErrorResp)],
    statusMarshaller: ToResponseMarshaller[StatusCode]): ToResponseMarshaller[AuthorizeError] =
    Marshaller {
      implicit ex: ExecutionContext => {
        case _: MustProvideLocationReferences.type => errorMarshaller(OK -> ErrorResp(NotEnoughInformation))
        case _: TokenNotFound.type => statusMarshaller(NotFound)
      }
    }

  // akka-http doesn't handle optional entity, see https://github.com/akka/akka-http/issues/284
  def optionalEntity[T](unmarshaller: FromRequestUnmarshaller[T]): Directive1[Option[T]] =
    entity(as[String]).flatMap { stringEntity =>
      if(stringEntity == null || stringEntity.isEmpty) {
        provide(Option.empty[T])
      } else {
        entity(unmarshaller).flatMap(e => provide(Some(e)))
      }
    }

  def route(apiUser: GlobalPartyId)(implicit ec: ExecutionContext) =
    get {
      pathEndOrSingleSlash {
        paged { (pager: Pager, dateFrom: Option[DateTime], dateTo: Option[DateTime]) =>
          onSuccess(service.tokens(pager, dateFrom, dateTo)) { pagTokens =>
            respondWithPaginationHeaders(pager, pagTokens ) {
              complete(SuccessWithDataResp(GenericSuccess, data = pagTokens.result))
            }
          }
        }
      }
    } ~
    pathPrefix(Segment) { tokenUid =>
      path("authorize") {
        (post & optionalEntity(as[LocationReferences])) { lr =>
          complete {
            service.authorize(tokenUid, lr).mapRight { authInfo =>
              SuccessWithDataResp(GenericSuccess, data = authInfo)
            }
          }
        }
      }
    }
}

