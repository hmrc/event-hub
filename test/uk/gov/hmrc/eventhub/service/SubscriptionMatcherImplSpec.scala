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

import cats.syntax.either._
import org.mockito.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.eventhub.config.TestModels.{EmailTopic, anotherSubscriber, channelPreferences, emails}
import uk.gov.hmrc.eventhub.config.Topic
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.eventhub.model.{Event, NoEventTopic, NoSubscribersForTopic, SubscriberRepository}
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.mongo.workitem.WorkItemRepository

class SubscriptionMatcherImplSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  behavior of "SubscriptionMatcherImpl.apply"

  it should "return subscribers that belong to the passed in topic and either have a JsPath filter that matches" +
    " the event or have no JsPath filter" in new Scope {
      subscriptionMatcherImpl.apply(event, EmailTopic) shouldBe subscriberRepos.asRight
    }

  it should "return NoEventTopic error when the topic does not exist" in new Scope {
    subscriptionMatcherImpl.apply(event, "nope") shouldBe NoEventTopic("No such topic").asLeft
  }

  it should "return NoSubscribersForTopic error when the topic exists but there are no matching subscribers" in new Scope {
    mongoSetup.topics returns Set(Topic(EmailTopic, List(channelPreferences)))

    subscriptionMatcherImpl.apply(
      event.copy(
        event = JsObject.apply(Map("foo" -> JsString("bar")))
      ),
      EmailTopic
    ) shouldBe NoSubscribersForTopic("No subscribers for topic").asLeft
  }

  trait Scope {
    val subscriberRepos = Set(
      SubscriberRepository(EmailTopic, channelPreferences, mock[WorkItemRepository[Event]]),
      SubscriberRepository(EmailTopic, anotherSubscriber, mock[WorkItemRepository[Event]])
    )

    val mongoSetup: MongoSetup = mock[MongoSetup]
    mongoSetup.topics returns Set(emails)
    mongoSetup.subscriberRepositories returns subscriberRepos

    val subscriptionMatcherImpl: SubscriptionMatcherImpl = new SubscriptionMatcherImpl(mongoSetup)
  }
}
