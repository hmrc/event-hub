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

package uk.gov.hmrc.eventhub.config

import akka.http.scaladsl.model.HttpMethods.PUT
import akka.http.scaladsl.model.Uri
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.TestModels.{subscriber, subscriptionDefaults}
import scala.concurrent.duration._

class TopicSpec extends AnyFlatSpec with Matchers {

  behavior of "Topic.configLoader"

  it should "load an empty set of topics from configuration" in {
    val config: Config = ConfigFactory.parseString("topics {}")
    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set.empty[Topic]
  }

  it should "load a topic with a subscriber only defining its `uri` property - all other properties should default" in {
    val config: Config = ConfigFactory.parseString(s"""
        |topics.email.${subscriber.name}.uri="${subscriber.uri}"
        |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber)))
  }

  it should "load a topic with a subscriber defining its `uri` & `http-method` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.http-method=PUT
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(httpMethod = PUT))))
  }

  it should "load a topic with a subscriber defining its `uri` & `elements` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.elements=0
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(elements = 0))))
  }

  it should "load a topic with a subscriber defining its `uri` & `per` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.per=5.seconds
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(per = 5.seconds))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-connections` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-connections=1
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(maxConnections = 1))))
  }

  it should "load a topic with a subscriber defining its `uri` & `min-back-off` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.http-method=PUT
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(httpMethod = PUT))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-back-off` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-back-off=1.hour
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(maxBackOff = 1.hour))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-retries` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-retries=0
         |""".stripMargin)

    val topic = Topic.configLoader(subscriptionDefaults).load(config, "topics").head

    topic.name shouldBe "email"
    val topicSubscriber = topic.subscribers.head
    topicSubscriber.name shouldBe "foo-subscriber"
    topicSubscriber.uri shouldBe Uri("http://localhost:8080/foo")
    topicSubscriber.maxRetries shouldBe 0
    topicSubscriber.maxBackOff shouldBe 2.seconds
    topicSubscriber.minBackOff shouldBe 1.seconds
  }

  it should "load a topic with a subscriber that has path defined" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.email.${subscriber.name}.uri="${subscriber.uri}"
                                                      |topics.email.${subscriber.name}.path="testPath"
                                                      |""".stripMargin)

    config.getValue("topics.email.foo-subscriber.path").toString should include("testPath")
  }

  it should "load a topic with a subscriber that has path None if its not defined" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.email.${subscriber.name}.uri="${subscriber.uri}"
                                                      |topics.email.${subscriber.name}.path="${subscriber.pathFilter}"
                                                      |""".stripMargin)

    config.getValue("topics.email.foo-subscriber.path").leftSide.toString should include("None")
  }

  it should "load a complex set of topics" in new Scope {
    val config: Config = ConfigFactory.parseString(complexConfig)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe complexTopics
  }

  it should "load a topic with no subscribers" in {
    val config: Config = ConfigFactory.parseString(
      s"""
        |topics.email=""
        |""".stripMargin
    )

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", Nil))
  }

  it should "fail to load when a subscribers uri property is not defined" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.http-method="POST"
         |topics.email.${subscriber.name}.elements="foo"
         |topics.email.${subscriber.name}.per="bar"
         |""".stripMargin)

    the[IllegalArgumentException] thrownBy Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") should have message s"""at 'email.foo-subscriber.elements':
        |  - (String: 3) Expected type NUMBER. Found STRING instead.
        |at 'email.foo-subscriber.per':
        |  - (String: 4) Cannot convert 'bar' to Duration: format error bar. (try a number followed by any of ns, us, ms, s, m, h, d).
        |at 'email.foo-subscriber.uri':
        |  - Unknown key.""".stripMargin
  }

  trait Scope {
    val subscriberTwo: Subscriber = subscriber.copy(name = "foo-two")
    val subscriberThree: Subscriber = subscriber.copy(name = "foo-three")
    val subscriberFour: Subscriber = subscriber.copy(name = "foo-four")

    val complexConfig: String =
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriberTwo.name}.uri="${subscriberTwo.uri}"
         |topics.letter.${subscriberThree.name}.uri="${subscriberThree.uri}"
         |topics.telephone.${subscriberFour.name}.uri="${subscriberFour.uri}"
         |topics.empty=""
         |""".stripMargin

    val complexTopics: Set[Topic] =
      Set(
        Topic("email", List(subscriber, subscriberTwo)),
        Topic("letter", List(subscriberThree)),
        Topic("telephone", List(subscriberFour)),
        Topic.empty("empty")
      )
  }
}
