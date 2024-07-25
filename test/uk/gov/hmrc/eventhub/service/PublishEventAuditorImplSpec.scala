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

import org.mockito.Mockito.{times, verify}
import org.mockito.ArgumentMatchers.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class PublishEventAuditorImplSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  behavior of "PublishEventAuditorImpl.failed"

  it should "raises a new DataEvent object from an Event and Exception" in new Scope {
    val publishEventAuditorImpl = new PublishEventAuditorImpl(auditMock)
    publishEventAuditorImpl.failed(event, exception)
    verify(auditMock, times(1)).sendDataEvent(any[DataEvent])(any[ExecutionContext])
  }

  trait Scope {
    val auditMock: Audit = mock[Audit]
    val func: DataEvent => Unit = mock[DataEvent => Unit]
    val exception = new RuntimeException("boom!")
  }
}
