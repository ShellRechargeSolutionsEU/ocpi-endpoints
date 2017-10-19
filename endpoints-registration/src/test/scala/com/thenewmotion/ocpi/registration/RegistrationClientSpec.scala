package com.thenewmotion.ocpi
package registration

import scala.concurrent.Future
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId, Url}
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.registration.RegistrationError.{SendingCredentialsFailed, UpdatingCredentialsFailed, VersionDetailsRetrievalFailed, VersionsRetrievalFailed}
import org.specs2.matcher.{EitherMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.specification.core.Env

class RegistrationClientSpec(environment: Env)
  extends Specification
  with Mockito
  with FutureMatchers
  with EitherMatchers {

  implicit val ee = environment.executionEnv
  implicit val ec = environment.executionContext
  implicit val mat: ActorMaterializer = null

  val uri = Uri("http://localhost/nothingHere")
  val ourToken = AuthToken[Ours]("token")
  val creds = Creds[Ours](
    AuthToken[Theirs]("token"),
    Url("http://localhost/norHere"),
    BusinessDetails("someOne", None, None),
    GlobalPartyId("party")
  )

  "Registration client recovers errors when" >> {
    "getting their versions" >> new TestScope {
      client.getTheirVersions(uri, ourToken) must beLeft(VersionsRetrievalFailed: RegistrationError).await
    }
    "getting their version details" >> new TestScope {
      client.getTheirVersionDetails(uri, ourToken) must beLeft(VersionDetailsRetrievalFailed: RegistrationError).await
    }
    "sending credentials" >> new TestScope {
      client.sendCredentials(Url("url"), ourToken, creds) must beLeft(SendingCredentialsFailed: RegistrationError).await
    }
    "updating credentials" >> new TestScope {
      client.updateCredentials(Url("url"), ourToken, creds) must beLeft(UpdatingCredentialsFailed: RegistrationError).await
    }
  }

  trait TestScope extends Scope {
    implicit val httpExt = mock[HttpExt]
    httpExt.singleRequest(any(), any(), any(), any())(any()) returns Future.failed(new RuntimeException)

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import com.thenewmotion.ocpi.msgs.v2_1.CredentialsJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_1.VersionsJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._

    val client = new RegistrationClient()
  }
}
