package com.thenewmotion.ocpi.registration

import scala.concurrent.Future

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId}
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.registration.RegistrationError.{SendingCredentialsFailed, UpdatingCredentialsFailed, VersionDetailsRetrievalFailed, VersionsRetrievalFailed}
import org.specs2.matcher.{DisjunctionMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.specification.core.Env

class RegistrationClientSpec(environment: Env)
  extends Specification
  with Mockito
  with FutureMatchers
  with DisjunctionMatchers {

  implicit val ee = environment.executionEnv
  implicit val ec = environment.executionContext
  implicit val mat: ActorMaterializer = null

  val uri = Uri("http://localhost/nothingHere")
  val ourToken = AuthToken[Ours]("token")
  val creds = Creds[Ours](
    AuthToken[Theirs]("token"),
    "http://localhost/norHere",
    BusinessDetails("someOne", None, None),
    GlobalPartyId("party")
  )

  "Registration client recovers errors when" >> {
    "getting their versions" >> new TestScope {
      client.getTheirVersions(uri, ourToken) must be_-\/(VersionsRetrievalFailed: RegistrationError).await
    }
    "getting their version details" >> new TestScope {
      client.getTheirVersionDetails(uri, ourToken) must be_-\/(VersionDetailsRetrievalFailed: RegistrationError).await
    }
    "sending credentials" >> new TestScope {
      client.sendCredentials("url", ourToken, creds) must be_-\/(SendingCredentialsFailed: RegistrationError).await
    }
    "updating credentials" >> new TestScope {
      client.updateCredentials("url", ourToken, creds) must be_-\/(UpdatingCredentialsFailed: RegistrationError).await
    }
  }

  trait TestScope extends Scope {
    implicit val httpExt = mock[HttpExt]
    httpExt.singleRequest(any, any, any, any)(any) returns Future.failed(new RuntimeException)
    val client = new RegistrationClient()
  }
}
