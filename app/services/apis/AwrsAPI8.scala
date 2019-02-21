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
import models.{WithdrawalReason, WithdrawalResponse}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import services.KeyStoreService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AccountUtils

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


trait AwrsAPI8 {
  val awrsConnector: AWRSConnector
  val keyStoreService: KeyStoreService

  def withdrawApplication(reason: Option[WithdrawalReason])(implicit hc: HeaderCarrier, user: AuthContext, request: Request[AnyContent]): Future[WithdrawalResponse] =
    reason match {
      case Some(resultReason) =>
        awrsConnector.withdrawApplication(AccountUtils.getAwrsRefNo.utr, Json.toJson(resultReason))
      case _ =>
        Future.failed(new NoSuchElementException("KeyStore is empty"))
    }
}

object AwrsAPI8 extends AwrsAPI8 {
  override val awrsConnector: AWRSConnector = AWRSConnector
  override val keyStoreService: KeyStoreService = KeyStoreService
}
