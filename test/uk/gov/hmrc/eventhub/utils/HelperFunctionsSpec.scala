/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eventhub.utils

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.model.UnknownError

import scala.concurrent.Future

class HelperFunctionsSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "HelperFunctions.liftFuture"

  it should "return Future[Either[_, A]] when presented with Either[_, Future[A]]" in {
    val eitherFutureOfA = Right(Future.successful("yay!"))
    val futureOfEitherOfA = HelperFunctions.liftFuture(eitherFutureOfA)(scala.concurrent.ExecutionContext.global)
    futureOfEitherOfA.futureValue should be(Right("yay!"))
  }

  it should "return Future[Either[PublishError, _]] when presented with Either[PublishError, Future[_]]" in {
    val leftEither = Left(UnknownError("boom!"))
    val futureOfEitherOfA = HelperFunctions.liftFuture(leftEither)(scala.concurrent.ExecutionContext.global)
    futureOfEitherOfA.futureValue should be(Left(UnknownError("boom!")))
  }
}
