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

package uk.gov.hmrc.eventhub.model

import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.model.TestModels.{event, publishedEvent}
import java.time.Instant

class PublishedEventSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "PublishedEvent.from"

  it should "lift a new PublishedEvent object from an Event one" in {
    val beforeInstantiation = Instant.now
    val sutPublishedEvent = PublishedEvent.from(event)
    val afterInstantiation = Instant.now
    (sutPublishedEvent.createdAt.toEpochMilli >= beforeInstantiation.toEpochMilli) should be(true)
    (sutPublishedEvent.createdAt.toEpochMilli <= afterInstantiation.toEpochMilli) should be(true)
    sutPublishedEvent.eventId should be(event.eventId)
    sutPublishedEvent.subject should be(event.subject)
    sutPublishedEvent.groupId should be(event.groupId)
    sutPublishedEvent.timestamp should be(event.timestamp)
    sutPublishedEvent.event should be(event.event)
  }

  behavior of "PublishedEvent.to"

  it should "lift a new Event object from an PublishedEvent one" in {
    val pubEvent = publishedEvent(createdAt = Instant.now)
    val sutEvent = PublishedEvent.to(pubEvent)
    sutEvent.eventId should be(pubEvent.eventId)
    sutEvent.subject should be(pubEvent.subject)
    sutEvent.groupId should be(pubEvent.groupId)
    sutEvent.timestamp should be(pubEvent.timestamp)
    sutEvent.event should be(pubEvent.event)
  }
}
