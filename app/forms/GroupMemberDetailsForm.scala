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

package forms

import forms.AWRSEnums.BooleanRadioEnum
import forms.prevalidation._
import forms.submapping.AddressMapping._
import forms.submapping.CompanyRegMapping._
import forms.submapping._
import forms.submapping.CompanyNamesMapping._
import forms.validation.util.ConstraintUtil._
import forms.validation.util.ErrorMessagesUtilAPI._
import forms.validation.util.MappingUtilAPI._
import forms.validation.util.NamedMappingAndUtil._
import forms.validation.util.TargetFieldIds
import models.GroupMember
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import utils.AwrsFieldConfig
import utils.AwrsValidator._

object GroupMemberDetailsForm {
  @inline def answeredYesToDoYouHaveCRN = whenAnswerToFieldIs(doYouHaveCrn, BooleanRadioEnum.YesString)(_)

  @inline def answeredYesToDoYouHaveUTR = whenAnswerToFieldIs(doYouHaveUtr, BooleanRadioEnum.YesString)(_)

  private val otherMembers_compulsory = yesNoQuestion_compulsory("addAnotherGrpMember", "awrs.group_member.addAnother.empty")
  private val mustHaveVRNorCRNorUTR = mustHaveAtLeastOneId(TargetFieldIds("doYouHaveVRN", "isBusinessIncorporated", "doYouHaveUTR"), "awrs.generic.error.identification_provided")

  private val companyNameIsEmpty = noAnswerGivenInField("names.companyName")
  private val tradingNameIsEmpty = noAnswerGivenInField("names.tradingName")

  val doYouHaveVrn = "doYouHaveVRN"
  val doYouHaveCrn = "isBusinessIncorporated"
  val doYouHaveUtr = "doYouHaveUTR"
  val utr = "utr"
  val vrn = "vrn"
  val crnMapping = "companyRegDetails"
  val names = "companyNames"

  private val companyNameAndTradingNameCannotBothBeEmpty =
    CrossFieldConstraint(
      companyNameIsEmpty &&& tradingNameIsEmpty,
      simpleCrossFieldErrorMessage(TargetFieldIds("names.companyName", "names.tradingName"),
        "awrs.generic.error.company_trading_name"))

  private def companyName_optional: Mapping[Option[String]] = {
    val fieldId = "names.companyName"
    val fieldNameInErrorMessage = "business name"
    val companyNameConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(AwrsFieldConfig.companyNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(companyNameConstraintParameters)
  }

  private def tradingName_optional: Mapping[Option[String]] = {
    val fieldId = "names.tradingName"
    val fieldNameInErrorMessage = "trading name"
    val companyNameConstraintParameters =
      OptionalTextFieldMappingParameter(
        genericFieldMaxLengthConstraintParameter(AwrsFieldConfig.tradingNameLen, fieldId, fieldNameInErrorMessage),
        genericInvalidFormatConstraintParameter(validText, fieldId, fieldNameInErrorMessage)
      )
    optionalText(companyNameConstraintParameters)
  }

  private val inferBasedOn = (dependentField: Option[String]) => dependentField.map(x => x.trim) match {
    case None | Some("") => BooleanRadioEnum.NoString
    case _ => BooleanRadioEnum.YesString
  }

  private val inferDoYouHave = (data: Map[String, String]) => {

    val doYouHaveCrnInferred = inferBasedOn(data.get(crnMapping attach crn))

    val doYouHaveUtrInferred = inferBasedOn(data.get(utr))

    data.+((doYouHaveCrn, doYouHaveCrnInferred), (doYouHaveUtr, doYouHaveUtrInferred))
  }

  /*
  *  1 map ids to if they are answered with yes
  *  2 combine the answers to get the boolean for if any id is answered with yes
  *  3 invert the aforementioned boolean
  *  4 check if vrn is answered
  */
  @inline def noIdsHaveBeenSupplied(data: FormData): Boolean = {
    val ids = Seq[String](doYouHaveUtr, doYouHaveCrn)
    val yesToDoYouHaveVRN = whenAnsweredYesToVRN(data)
    val vrnIsNotAnswered = data.getOrElse(vrn, "").equals("")
    !ids.filter(!_.equals(doYouHaveVrn)).map(id => whenAnswerToFieldIs(id, BooleanRadioEnum.YesString)(data)).reduce(_ || _) &&
      (!yesToDoYouHaveVRN || yesToDoYouHaveVRN && vrnIsNotAnswered) // if doYouhaveVRN is not answered or if it is answered with yes but no VRN is supplied
  }

  val groupMemberValidationForm = {
    val noIdIsSupplied = noIdsHaveBeenSupplied(_)
    Form(
      mapping(
        names -> companyNamesMapping(names),
        "address" -> ukAddress_compulsory(prefix = "address").toOptionalAddressMapping,
        "groupJoiningDate" -> optional(text),
        doYouHaveUtr -> doYouHaveUTR_compulsory(doYouHaveUtr),
        utr -> (utr_compulsory(utr) iff noIdIsSupplied ||| answeredYesToDoYouHaveUTR),
        doYouHaveCrn -> doYouHaveCRN_compulsory(doYouHaveCrn),
        crnMapping -> (companyReg_compulsory(crnMapping).toOptionalCompanyRegMapping iff noIdIsSupplied ||| answeredYesToDoYouHaveCRN),
        doYouHaveVrn -> (doYouHaveVRN_compulsory() + mustHaveVRNorCRNorUTR),
        vrn -> (vrn_compulsory() iff whenAnsweredYesToVRN),
        "addAnotherGrpMember" -> otherMembers_compulsory
      )
      (GroupMember.apply)(GroupMember.unapply)
    )
  }

  val groupMemberForm = PreprocessedForm(groupMemberValidationForm).addNewPreprocessFunction(inferDoYouHave)

}
