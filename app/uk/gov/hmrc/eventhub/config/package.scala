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

package uk.gov.hmrc.eventhub

import cats.kernel.Semigroup
import pureconfig.error.ConfigReaderFailures

package object config {

  /** Required for combining config reader failures when traversing and parsing topic configs on application startup.
    * Anywhere cats uses an applicative to combine effects (product, mapN, tupleN, traverse etc) knowledge of how to
    * combine (semigroup or monoid) the potential result type is required to be in implicit scope, for an either one of
    * these is required for the left hand side also.
    */
  implicit object semigroupConfigReaderFailures extends Semigroup[ConfigReaderFailures] {
    override def combine(x: ConfigReaderFailures, y: ConfigReaderFailures): ConfigReaderFailures = x.++(y)
  }
}
