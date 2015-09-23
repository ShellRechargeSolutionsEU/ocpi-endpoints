package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.handshake.Errors.{VersionDetailsRetrievalFailed, VersionsRetrievalFailed, HandshakeError}
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{Creds => OcpiCredentials}
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import org.joda.time.format.ISODateTimeFormat
import org.specs2.matcher.{DisjunctionMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.Uri
import scala.concurrent.Future
import scalaz._

class HandshakeServiceSpec extends Specification  with Mockito with FutureMatchers
  with DisjunctionMatchers{

  "HandshakeService should" should {
    "return credentials with new token if the initiating partie's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds)

      result must beLike[\/[HandshakeError, OcpiCredentials]] {
        case \/-(OcpiCredentials(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual OcpiBusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error if there was an error getting versions" in new HandshakeTestScope {
      client.getVersions(clientCreds.versions_url, currentAuth) returns Future.successful(-\/(VersionsRetrievalFailed))
      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds)

      result must be_-\/.await
    }

    "return error if no versions were returned" in new HandshakeTestScope {
      client.getVersions(clientCreds.versions_url, currentAuth) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
        List())))
      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds)

      result must be_-\/.await
    }

    "return error if there was an error getting version details" in new HandshakeTestScope {
      client.getVersionDetails(clientVersionDetailsUrl, currentAuth) returns Future.successful(
        -\/(VersionDetailsRetrievalFailed))

      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds)

      result must be_-\/.await
    }

  }

  trait HandshakeTestScope extends Scope {

    val selectedVersion = "2.0"
    val currentAuth = "123"
    val newAuth = "456"
    val clientVersionsUrl = "http://the-awesomes/msp/versions"
    val clientVersionDetailsUrl = "http://the-awesomes/msp/2.0"
    val clientCreds = Credentials(
      newAuth,
      clientVersionsUrl,
      BusinessDetails(
        "The Awesomes",
        None,
        None
      )
    )

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val dateTime1 = formatter.parseDateTime("2010-01-01T00:00:00Z")

    val cdh = mock[HandshakeDataHandler]
    cdh.persistClientPrefs(any, any, any) returns \/-(Unit)
    cdh.config returns HandshakeConfig("localhost", 8080, "TNM (CPO)","cpo","","credentials","versions")

    var client = mock[HandshakeClient]

    client.getVersions(clientCreds.versions_url, currentAuth) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", clientVersionDetailsUrl)))))
    client.getVersionDetails(clientVersionDetailsUrl, currentAuth) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",
        List(Endpoint(EndpointIdentifierEnum.Credentials, clientVersionDetailsUrl + "/credentials"))))))
    val handshakeService = new HandshakeService(client, cdh)

  }
}
