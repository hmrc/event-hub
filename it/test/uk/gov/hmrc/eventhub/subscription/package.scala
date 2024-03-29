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

import org.apache.pekko.http.scaladsl.model.StatusCode
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson}
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic, TopicName}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.subscription.model.TestTopic

package object subscription {
  implicit class TopicOps(val topic: Topic) extends AnyVal {
    def returning(statusCode: StatusCode): TestTopic = TestTopic(topic, statusCode)
  }

  implicit class TopicsOps(val topics: Set[Topic]) extends AnyVal {
    def returning(statusCode: StatusCode): Set[TestTopic] = topics.map(_.returning(statusCode))
  }

  implicit class SubscriberConfigOps(val subscriber: Subscriber) extends AnyVal {
    def asConfigMap(topicName: TopicName): Map[String, Any] =
      Map(
        s"${topicName.name}.${subscriber.name}.uri"             -> subscriber.uri.toString(),
        s"${topicName.name}.${subscriber.name}.http-method"     -> subscriber.httpMethod.value,
        s"${topicName.name}.${subscriber.name}.elements"        -> subscriber.elements,
        s"${topicName.name}.${subscriber.name}.per"             -> subscriber.per.toString(),
        s"${topicName.name}.${subscriber.name}.max-connections" -> subscriber.maxConnections,
        s"${topicName.name}.${subscriber.name}.min-back-off"    -> subscriber.minBackOff.toString(),
        s"${topicName.name}.${subscriber.name}.max-back-off"    -> subscriber.maxBackOff.toString(),
        s"${topicName.name}.${subscriber.name}.max-retries"     -> subscriber.maxRetries
      ) ++
        subscriber
          .pathFilter
          .map(filter => Map(s"${topicName.name}.${subscriber.name}.filter-path" -> filter.getPath))
          .getOrElse(Map.empty)
  }

  implicit class RequestPatternBuilderOps(val requestPatternBuilder: RequestPatternBuilder) extends AnyVal {
    def withEventJson(event: Event): RequestPatternBuilder =
      requestPatternBuilder
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.toJson(event).toString()))
  }
}
