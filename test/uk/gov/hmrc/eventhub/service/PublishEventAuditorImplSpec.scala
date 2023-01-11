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

package uk.gov.hmrc.eventhub.service

import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

class PublishEventAuditorImplSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  behavior of "PublishEventAuditorImpl.failed"

  it should "raises a new DataEvent object from an Event and Exception" in new Scope {
    func.apply(*[DataEvent]) returns Unit
    auditMock.sendDataEvent returns func
    val publishEventAuditorImpl = new PublishEventAuditorImpl(auditMock)
    publishEventAuditorImpl.failed(event, exception)
    auditMock.sendDataEvent(*[DataEvent]) wasCalled once
    func.apply(*[DataEvent]) wasCalled once
  }

  trait Scope {
    val auditMock: Audit = mock[Audit]
    val func: DataEvent => Unit = mock[DataEvent => Unit]
    val exception = new RuntimeException("boom!")
  }
}
