/*
 * Copyright 2018 HM Revenue & Customs
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

package views.view_application

import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.twirl.api.{Html, HtmlFormat}
import services.DataCacheKeys._
import utils.CacheUtil
import views.view_application.helpers.SubViewTemplateHelper._
import views.view_application.helpers.ViewApplicationType
import scala.language.implicitConversions
import scala.xml.Elem

object ViewApplicationHelper {

  val NoneBreakingSpace = "\u00A0"

  implicit val cacheUtil = CacheUtil.cacheUtil

  implicit class OptionStringUtil(someStr: Option[String]) {
    def `+`(anotherSomeStr: Option[String]): Option[String] = (someStr, anotherSomeStr) match {
      case (None, None) => None
      case _ =>
        Some(someStr.fold("")(x => x) + anotherSomeStr.fold("")(x => x))
    }
  }

  implicit class StringUtil(str: String) {
    // This class is created because:
    // " " + Some(string) will return " Some(string)"
    // This is due to + being defined in the String class itself and hence will take precedence over any definitions
    // written here.
    // the x symbol function is defined for the cases where the desired concatation needs to be led by a string
    def `x`(anotherSomeStr: Option[String]): Option[String] = anotherSomeStr match {
      case None => str
      case _ =>
        Some(str + anotherSomeStr.fold("")(x => x))
    }
  }

  def hasContent(rows: Traversable[Option[String]]): Boolean =
    rows.foldLeft(false)((bool, r) => bool || r.isDefined)

  def countContent(rows: Traversable[Option[String]]): Int =
    rows.foldLeft(0)(
      (count, r) =>
        count + (r match {
          case Some(str) if !str.equals("") => 1
          case _ => 0
        }))

  implicit def someConversion[A](any: A): Option[A] = Some(any)

  implicit def intConversionToString(int: Int): Option[String] = Some(int.toString)

  implicit def htmlToString(html: Html): String = html.toString()

  implicit def appendableToOptionString(html: HtmlFormat.Appendable): Option[String] = Some(HtmlFormat.raw(html))

  implicit def stringToHtml(str: String): Html = Html(str)

  implicit def elemToHtml(elem: Elem): Option[Html] = Some(Html(elem.toString()))

  def link(href: Option[String], message: String, classAttr: String, idAttr: Option[String] = None, visuallyHidden: String = ""): String = {
    "<a " + {
      idAttr match {
        case Some(id) => "id=\"" + id + "\""
        case _ => ""
      }
    } + " class=\"" + classAttr + "\" href=\"" + href.get + "\">" + message + {
      visuallyHidden match {
        case "" => ""
        case text => "<span class=\"visuallyhidden\">" + text + "</span>"
      }
    } + "</a>"
  }

  def edit_link(editUrl: Int => String, id: Int, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType) = {

    if (isSectionEdit) {
      link(
        Some(editUrl(id)),
        Messages("awrs.view_application.edit"),
        classAttr = "edit-link",
        idAttr = "edit-" + id,
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }

  def edit_link_s(editUrl: String, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType) = {

    if (isRecordEdit) {
      link(
        Some(editUrl),
        Messages("awrs.view_application.edit"),
        classAttr = "edit-link",
        idAttr = "edit-link",
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }

  def delete_link(deleteUrl: Int => String, id: Int, visuallyHidden: String = "")(implicit viewApplicationType: ViewApplicationType) = {

    if (isSectionEdit) {
      link(
        Some(deleteUrl(id)),
        Messages("awrs.view_application.delete"),
        classAttr = "edit-link",
        idAttr = "delete-" + id,
        visuallyHidden = visuallyHidden
      )
    } else {
      NoneBreakingSpace
    }
  }

  implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci = ( "(?i)" + sc.parts.mkString ).r
  }

  def getSectionDisplayName(sectionName: String, legalEntity: String): Option[String] = {
    (sectionName, legalEntity) match {
      case (`businessDetailsName`, "Partnership" | "LP" | "LLP") => Messages("awrs.view_application.partnership_details_text")
      case (`businessDetailsName`, "LLP_GRP" | "LTD_GRP") => Messages("awrs.view_application.group_business_details_text")
      case (`businessDetailsName`, _) => Messages("awrs.view_application.business_details_text")
      case (`businessRegistrationDetailsName`, "Partnership" | "LP" | "LLP") => Messages("awrs.view_application.partnership_registration_details_text")
      case (`businessRegistrationDetailsName`, "LLP_GRP" | "LTD_GRP") => Messages("awrs.view_application.group_business_registration_details_text")
      case (`businessRegistrationDetailsName`, _) => Messages("awrs.view_application.business_registration_details_text")
      case (`placeOfBusinessName`, "Partnership" | "LP" | "LLP") => Messages("awrs.view_application.partnership_place_of_business_text")
      case (`placeOfBusinessName`, "LLP_GRP" | "LTD_GRP") => Messages("awrs.view_application.group_place_of_business_text")
      case (`placeOfBusinessName`, _) => Messages("awrs.view_application.place_of_business_text")
      case (`businessContactsName`, "Partnership" | "LP" | "LLP") => Messages("awrs.view_application.partnership_contacts_text")
      case (`businessContactsName`, "LLP_GRP" | "LTD_GRP") => Messages("awrs.view_application.group_business_contacts_text")
      case (`businessContactsName`, _) => Messages("awrs.view_application.business_contacts_text")
      case (`partnersName`, _) => Messages("awrs.view_application.business_partners_text")
      case (`groupMembersName`, _) => Messages("awrs.view_application.group_member_details_text")
      case (`additionalBusinessPremisesName`, _) => Messages("awrs.view_application.additional_premises_text")
      case (`businessDirectorsName`, _) => Messages("awrs.view_application.business_directors.index_text")
      case (`tradingActivityName`, _) => Messages("awrs.view_application.trading_activity_text")
      case (`productsName`, _) => Messages("awrs.view_application.products_text")
      case (`suppliersName`, _) => Messages("awrs.view_application.suppliers_text")
      case (`applicationDeclarationName`, _) => Messages("awrs.view_application.application_declaration_text")
      case _ => None
    }
  }

}
