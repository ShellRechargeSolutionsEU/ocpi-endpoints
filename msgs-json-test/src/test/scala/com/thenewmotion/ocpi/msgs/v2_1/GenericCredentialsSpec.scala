package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId, Url}
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials._
import org.specs2.specification.core.Fragments
import scala.language.higherKinds

trait GenericCredentialsSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit credsR: GenericJsonReader[Creds[Ours]],
    credsW: GenericJsonWriter[Creds[Ours]]
  ): Fragments = {
    "Creds" should {
      testPair(credentials1, parse(credentialsJson1))
    }
  }

  val businessDetails1 = BusinessDetails(
    "Example Operator",
    Some(Image(Url("http://example.com/images/logo.png"), ImageCategory.Operator, "png")),
    Some(Url("http://example.com"))
  )
  val credentials1 = Creds[Ours](
    token = AuthToken[Theirs]("ebf3b399-779f-4497-9b9d-ac6ad3cc44d2"),
    url = Url("https://example.com/ocpi/cpo/"),
    businessDetails = businessDetails1,
    globalPartyId = GlobalPartyId("NL", "EXA")
  )

  val logo1 = businessDetails1.logo.get
  val credentialsJson1 =
    s"""
       |{
       |    "token": "${credentials1.token.value}",
       |    "url": "${credentials1.url}",
       |    "business_details": {
       |        "name": "${credentials1.businessDetails.name}",
       |        "logo": {
       |          "url": "${logo1.url}",
       |          "category": "${logo1.category.name}",
       |          "type": "${logo1.`type`}"
       |        },
       |        "website": "${credentials1.businessDetails.website.get}"
       |    },
       |    "party_id": "${credentials1.globalPartyId.partyId}",
       |    "country_code": "${credentials1.globalPartyId.countryCode}"
       |}
   """.stripMargin
}
