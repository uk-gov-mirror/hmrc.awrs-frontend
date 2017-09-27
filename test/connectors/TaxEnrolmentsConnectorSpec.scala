/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import audit.TestAudit
import metrics.AwrsMetrics
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST => _, INTERNAL_SERVER_ERROR => _, NOT_FOUND => _, OK => _, SERVICE_UNAVAILABLE => _}
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import services.GGConstants._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http._
import utils.AwrsUnitTestTraits

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse }

class TaxEnrolmentsConnectorSpec extends AwrsUnitTestTraits {

  val MockAuditConnector = mock[AuditConnector]

  class MockHttp extends HttpGet with WSGet with HttpPost with WSPost with HttpAuditing {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = MockAuditConnector

    override def appName = "awrs-frontend"
  }

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestTaxEnrolmentsConnector extends TaxEnrolmentsConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
    override val metrics = AwrsMetrics
    override val audit: Audit = new TestAudit
  }

  "Tax enrolments connector de-enrolling AWRS" should {
    // used in the mock to check the destination of the connector calls
    lazy val deEnrolURI = TaxEnrolmentsConnector.deEnrolURI + "/" + service

    // these values doesn't really matter since the call itself is mocked
    val awrsRef = ""
    val businessName = ""
    val businessType = ""
    val deEnrolResponseSuccess = true
    val deEnrolResponseFailure = false

    def mockResponse(responseStatus: Int, responseString: Option[String] = None): Unit =
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.endsWith(deEnrolURI), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus = responseStatus, responseString = responseString)))

    def testCall(implicit headerCarrier: HeaderCarrier) = TestTaxEnrolmentsConnector.deEnrol(awrsRef, businessName, businessType)(headerCarrier)

    "return status as OK, for successful de-enrolment" in {
      mockResponse(OK)
      val result = testCall
      await(result) shouldBe deEnrolResponseSuccess
    }

    "return status as BAD_REQUEST, for unsuccessful de-enrolment" in {
      mockResponse(BAD_REQUEST)
      val result = testCall
      await(result) shouldBe deEnrolResponseFailure
    }

    "return status as NOT_FOUND, for unsuccessful de-enrolment" in {
      mockResponse(NOT_FOUND)
      val result = testCall
      await(result) shouldBe deEnrolResponseFailure
    }

    "return status as SERVICE_UNAVAILABLE, for unsuccessful de-enrolment" in {
      mockResponse(SERVICE_UNAVAILABLE)
      val result = testCall
      await(result) shouldBe deEnrolResponseFailure
    }

    "return status as INTERNAL_SERVER_ERROR, for unsuccessful de-enrolment" in {
      mockResponse(INTERNAL_SERVER_ERROR, Some("error in de-enrol service end point error"))
      val result = testCall
      await(result) shouldBe deEnrolResponseFailure
    }

    "return status as unexpected status, for unsuccessful de-enrolment" in {
      val otherStatus = 999
      mockResponse(otherStatus)
      val result = testCall
      await(result) shouldBe deEnrolResponseFailure
    }

  }

}
