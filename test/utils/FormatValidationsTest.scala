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

package utils

import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsValidator._

class FormatValidationsTest extends UnitSpec with Matchers {

  private def testRegex(regExPattern:String, validData: Seq[String], invalidData: Seq[String]): Unit = {
    withClue("the regex should allow these valid cases\n") {
      for (data <- validData)
        withClue(f"$data did not pass\n") {
          data.matches(regExPattern) shouldBe true
        }
    }
    withClue("the regex should reject these invalid cases\n") {
      for (data <- invalidData)
        withClue(f"$data did not pass\n") {
          data.matches(regExPattern) shouldBe false
        }
    }
  }

  private def testStringValidationFunction(function: (String) => Boolean, validData: Seq[String], invalidData: Seq[String]): Unit = {
    withClue("the regex should allow these valid cases\n") {
      for (data <- validData)
        withClue(f"$data did not pass\n") {
          validText(data) shouldBe true
        }
    }
    withClue("the regex should reject these invalid cases\n") {
      for (data <- invalidData)
        withClue(f"$data did not pass\n") {
          validText(data) shouldBe false
        }
    }
  }

  "alpha numeric reg ex" should {
    "satisfy the following valid and invalid cases" in {
      val validData = Seq("0", "9", "a", "A", "z", "Z")
      val invalidData = Seq("α", "&", "*", "@", "£")
      testRegex(alphaNumericRegex,validData, invalidData)
    }
  }

  "crn reg ex" should {
    "satisfy the following valid" in {
      Seq(
        "12345678",
        "01234567",
        "NI123456",
        "ni000000",
        "ni000000",
        "NO987654",
        "9876543"
      ) foreach { crn =>
        crn.matches(crnRegex) shouldBe true
      }
    }
    "satisfy the following invalid" in {
      Seq(
        "1",
        "123456",
        "123456789",
        "ABC12312",
        "12AB3456",
        " 1234567",
        "123456  ",
        "A1234567",
        "SC 236367"
      ) foreach { crn =>
        crn.matches(crnRegex) shouldBe false
      }
    }
  }

  "validation functions" should {
    "satisfy the following valid and invalid cases" in {
      val validData = Seq("0", "9", "a", "A", "z", "Z")
      val invalidData = Seq("α")
      testStringValidationFunction(validText, validData, invalidData)
    }
  }
}
