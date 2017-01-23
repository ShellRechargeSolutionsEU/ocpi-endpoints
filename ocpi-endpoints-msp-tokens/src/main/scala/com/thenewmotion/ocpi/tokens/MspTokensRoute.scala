package com.thenewmotion.ocpi.tokens

import com.thenewmotion.ocpi.common.{Pager, PaginatedRoute}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.SuccessWithDataResp
import com.thenewmotion.ocpi.{ApiUser, JsonApi}
import org.joda.time.DateTime
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode._

class MspTokensRoute(
  service: MspTokensService,
  val DefaultLimit: Int = 1000,
  val MaxLimit: Int = 1000
) extends JsonApi with PaginatedRoute {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def route(apiUser: ApiUser) =
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
  }
}

