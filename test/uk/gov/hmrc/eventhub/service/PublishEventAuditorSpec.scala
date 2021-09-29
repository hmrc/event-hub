/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.model.TestModels.event

class PublishEventAuditorSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  behavior of "PublishEventAuditor.asDataEvent"

  it should "lift a new DataEvent object from an Event and Exception" in {
    val sutDataEvent = PublishEventAuditor.asDataEvent(event, new RuntimeException("boom!"))
    sutDataEvent.auditSource should be("event-hub-publisher")
    sutDataEvent.auditType should be("TxFailed")
    sutDataEvent.detail("reason") should be("boom!")
    sutDataEvent.detail("subject") should be("\"foo bar\"")
    sutDataEvent.detail("groupId") should be("\"in the bar\"")
    sutDataEvent.detail("event").contains("failed") should be(true)
    sutDataEvent.detail("event").contains("hmrc-customer@some-domain.org") should be(true)
    sutDataEvent.detail("event").contains("2021-04-07T09:46:29+00:00") should be(true)
    sutDataEvent.detail("event").contains("605") should be(true)
    sutDataEvent.detail("event").contains("Not delivering to previously bounced address") should be(true)
    sutDataEvent.detail("event").contains("HMRC-CUS-ORG~EORINumber~1234") should be(true)
  }
}
