package com.thenewmotion.ocpi
package tokens

import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import common.{ErrRespUnMar, OcpiClient, PagedRespUnMar}
import java.time.ZonedDateTime
import msgs.{AuthToken, ErrorResp}
import msgs.v2_1.Tokens.Token
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TokensClient(
  implicit http: HttpExt,
  errorU: ErrRespUnMar,
  sucU: PagedRespUnMar[Token]
) extends OcpiClient {

  def getTokens(
    uri: Uri,
    auth: AuthToken[Ours],
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None,
    pageLimit: Int = OcpiClient.DefaultPageLimit
  )(implicit ec: ExecutionContext, mat: Materializer): Future[Either[ErrorResp, Iterable[Token]]] =
    traversePaginatedResource[Token](uri, auth, dateFrom, dateTo, pageLimit)
}
