package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import com.github.nscala_time.time.Imports._
import common.OcpiClient
import msgs.ErrorResp
import msgs.v2_1.Tokens.Token

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz._

class TokensClient(implicit http: HttpExt) extends OcpiClient {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import msgs.v2_1.OcpiJsonProtocol._

  def getTokens(uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer):
  Future[ErrorResp \/ Iterable[Token]] =
    traversePaginatedResource[Token](uri, auth, dateFrom, dateTo)
}
