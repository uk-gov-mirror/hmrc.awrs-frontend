/*
 * Copyright 2019 HM Revenue & Customs
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

package services.apis

import connectors.AWRSConnector
import models.FormBundleStatus._
import models.StatusContactType.MindedToReject
import models._
import play.api.mvc.{AnyContent, Request}
import services.KeyStoreService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AccountUtils, LoggingUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

// Unit tests for this class are covered in EtmpLookupService for retrieveApplication

trait AwrsAPI11 extends LoggingUtils {

  val awrsConnector: AWRSConnector
  val keyStoreService: KeyStoreService
  lazy val noStatusInfo = StatusInfoType(None)

  private def getAwrsRefNo(implicit user: AuthContext, hc: HeaderCarrier) = AccountUtils.getAwrsRefNo.toString()

  private def saveEmpty(implicit user: AuthContext, hc: HeaderCarrier) = keyStoreService.saveStatusInfo(noStatusInfo)
    .flatMap { _ => Future.successful(Some(noStatusInfo)) }

  def getStatusInfo(statusType: FormBundleStatus, someContactNumber: Option[String], someContactType: Option[StatusContactType])
                   (implicit hc: HeaderCarrier, user: AuthContext, request: Request[AnyContent]): Future[Option[StatusInfoType]] = {

    lazy val callAPI11 = someContactNumber match {
      case Some(contactNumber) =>
        getStatusInfoFromEtmp(contactNumber,statusType,someContactType)
      case _ =>
        val errorStr = "API11 call failure: No business contact number was supplied"
        err(errorStr)
        Future.failed(new BadRequestException(errorStr))
    }
    lazy val doNotCallAPI11 = Future.successful(None)

    keyStoreService.fetchStatusInfo flatMap {
      case None =>

        statusType match {
          case Approved | ApprovedWithConditions | Rejected | Revoked | RejectedUnderReviewOrAppeal | RevokedUnderReviewOrAppeal => callAPI11
          case Pending =>
            someContactType match {
              case Some(MindedToReject) => callAPI11
              case _ => doNotCallAPI11
            }
          case _ => doNotCallAPI11
        }
      case successData: Option[StatusInfoType] => Future.successful(successData)
    }
  }

  @inline def getStatusInfoFromCache(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Option[StatusInfoType]] =
    keyStoreService.fetchStatusInfo

  private def getStatusInfoFromEtmp(contactNumber: String,formBundleStatus: FormBundleStatus,statusContactType: Option[StatusContactType])(implicit user: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Option[StatusInfoType]] =
    awrsConnector.getStatusInfo(getAwrsRefNo, contactNumber,formBundleStatus, statusContactType) flatMap {
      case successData: StatusInfoType =>
        keyStoreService.saveStatusInfo(successData) flatMap { _ => Future.successful(Some(successData)) }
    }
}

object AwrsAPI11 extends AwrsAPI11 {
  override val awrsConnector = AWRSConnector
  override val keyStoreService = KeyStoreService
}
