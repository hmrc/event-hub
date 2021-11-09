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
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.Subscriber.{InvalidSubscriberName, InvalidTopicName}
import uk.gov.hmrc.eventhub.config.TestModels.{subscriber, subscriptionDefaults}

import scala.concurrent.duration._

class TopicSpec extends AnyFlatSpec with Matchers {

  behavior of "Subscriber.validateTopicName"

  it should "return a the topic name supplied back when provided with a topic's name which contains digits, hyphens, and lowercase letters" in {
    val topicName = TopicName(name = "topic01-23name")
    val result = Subscriber.validateTopicName(topicName)
    result shouldBe Right(topicName)
  }

  it should "return a failure reason when provided with a topic's name which contains special characters" in {
    val topicName = TopicName(name = "topic+++*&*&*^&^&%^@$^&%@_name")
    val result = Subscriber.validateTopicName(topicName)
    result shouldBe Left(InvalidTopicName(topicName))
    result.left.get.description shouldBe "Invalid topic name: topic+++*&*&*^&^&%^@$^&%@_name"
  }

  it should "return a failure reason when provided with a topic's name which contains capitals" in {
    val topicName = TopicName(name = "TopicName")
    val result = Subscriber.validateTopicName(topicName)
    result shouldBe Left(InvalidTopicName(topicName))
    result.left.get.description shouldBe "Invalid topic name: TopicName"
  }

  behavior of "Subscriber.validateSubscriberName"

  it should "return a the subscriber name supplied back when provided with a subscriber's name which contains digits, hyphens, and lowercase letters" in {
    val subscriberName = "subscriber01-23name"
    val result = Subscriber.validateSubscriberName(subscriberName)
    result shouldBe Right(subscriberName)
  }

  it should "return a failure reason when provided with a topic's name which contains special characters" in {
    val subscriberName = "subscriber+++*&*&*^&^&%^@$^&%@_name"
    val result = Subscriber.validateSubscriberName(subscriberName)
    result shouldBe Left(InvalidSubscriberName(subscriberName))
    result.left.get.description shouldBe "Invalid subscriber name: subscriber+++*&*&*^&^&%^@$^&%@_name"
  }

  it should "return a failure reason when provided with a topic's name which contains capitals" in {
    val subscriberName = "SubscriberName"
    val result = Subscriber.validateSubscriberName(subscriberName)
    result shouldBe Left(InvalidSubscriberName(subscriberName))
    result.left.get.description shouldBe "Invalid subscriber name: SubscriberName"
  }

  behavior of "Topic.configLoader"

  it should "load an empty set of topics from configuration" in {
    val config: Config = ConfigFactory.parseString("topics {}")
    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set.empty[Topic]
  }

  it should "throw an exception when loading a topic with a topic's name which contains special characters" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.~~~.${subscriber.name}.uri="${subscriber.uri}"
                                                      |""".stripMargin)

    the[IllegalArgumentException] thrownBy {
      Topic
        .configLoader(subscriptionDefaults)
        .load(config, "topics") shouldBe Set(Topic(TopicName("~~~"), List(subscriber)))
    } should have message "could not load subscription configuration: Invalid topic name: ~~~"
  }

  it should "throw an exception when loading a topic with a subscriber's name which contains special characters" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.email.~~~.uri="${subscriber.uri}"
                                                      |""".stripMargin)

    the[IllegalArgumentException] thrownBy {
      Topic
        .configLoader(subscriptionDefaults)
        .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber)))
    } should have message "could not load subscription configuration: Invalid subscriber name: ~~~"
  }

  it should "throw an exception when loading a topic with a topic's name which has capitals" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.Email.${subscriber.name}.uri="${subscriber.uri}"
                                                      |""".stripMargin)

    the[IllegalArgumentException] thrownBy {
      Topic
        .configLoader(subscriptionDefaults)
        .load(config, "topics") shouldBe Set(Topic(TopicName("Email"), List(subscriber)))
    } should have message "could not load subscription configuration: Invalid topic name: Email"
  }

  it should "throw an exception when loading a topic with a subscriber's name which has capitals" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.email.${subscriber.name.toUpperCase}.uri="${subscriber.uri}"
                                                      |""".stripMargin)

    the[IllegalArgumentException] thrownBy {
      Topic
        .configLoader(subscriptionDefaults)
        .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber)))
    } should have message s"could not load subscription configuration: Invalid subscriber name: ${subscriber.name.toUpperCase}"
  }

  it should "load a topic with a subscriber and not throw an exception when using hyphens or numbers in the topic's name" in {
    val config: Config = ConfigFactory.parseString(s"""
                                                      |topics.0-1-2-3.${subscriber.name}.uri="${subscriber.uri}"
                                                      |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("0-1-2-3"), List(subscriber)))
  }

  it should "load a topic with a subscriber only defining its `uri` property - all other properties should default" in {
    val config: Config = ConfigFactory.parseString(s"""
        |topics.email.${subscriber.name}.uri="${subscriber.uri}"
        |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber)))
  }

  it should "load a topic with a subscriber defining its `uri` & `http-method` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.http-method=PUT
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(httpMethod = PUT))))
  }

  it should "load a topic with a subscriber defining its `uri` & `elements` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.elements=0
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(elements = 0))))
  }

  it should "load a topic with a subscriber defining its `uri` & `per` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.per=5.seconds
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(per = 5.seconds))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-connections` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-connections=1
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(maxConnections = 1))))
  }

  it should "load a topic with a subscriber defining its `uri` & `min-back-off` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.http-method=PUT
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(httpMethod = PUT))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-back-off` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-back-off=1.hour
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(maxBackOff = 1.hour))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-retries` properties" in {
    val config: Config = ConfigFactory.parseString(s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-retries=0
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), List(subscriber.copy(maxRetries = 0))))
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
      .load(config, "topics") shouldBe Set(Topic(TopicName("email"), Nil))
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
        Topic(TopicName("email"), List(subscriber, subscriberTwo)),
        Topic(TopicName("letter"), List(subscriberThree)),
        Topic(TopicName("telephone"), List(subscriberFour)),
        Topic.empty(TopicName("empty"))
      )
  }
}
