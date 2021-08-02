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
    val config: Config = ConfigFactory.parseString(
      s"""
        |topics.email.${subscriber.name}.uri="${subscriber.uri}"
        |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber)))
  }

  it should "load a topic with a subscriber defining its `uri` & `http-method` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.http-method=PUT
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(httpMethod = PUT))))
  }
  
  it should "load a topic with a subscriber defining its `uri` & `elements` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.elements=0
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(elements = 0))))
  }

  it should "load a topic with a subscriber defining its `uri` & `per` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.per=5.seconds
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(per = 5.seconds))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-connections` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-connections=1
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(maxConnections = 1))))
  }

  it should "load a topic with a subscriber defining its `uri` & `min-back-off` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.http-method=PUT
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(httpMethod = PUT))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-back-off` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-back-off=1.hour
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(maxBackOff = 1.hour))))
  }

  it should "load a topic with a subscriber defining its `uri` & `max-retries` properties" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.uri="${subscriber.uri}"
         |topics.email.${subscriber.name}.max-retries=0
         |""".stripMargin)

    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe Set(Topic("email", List(subscriber.copy(maxRetries = 0))))
  }

  it should "load a complex set of topics" in new Scope {
    val config: Config = ConfigFactory.parseString(complexConfig)
    
    Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") shouldBe complexTopics
  }

  it should "fail to load when a subscribers uri property is not defined" in {
    val config: Config = ConfigFactory.parseString(
      s"""
         |topics.email.${subscriber.name}.http-method="POST"
         |""".stripMargin)

    the[IllegalArgumentException] thrownBy Topic
      .configLoader(subscriptionDefaults)
      .load(config, "topics") should have message s"at '${subscriber.name}.uri':\n  - Unknown key."
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
         |""".stripMargin
    
    val complexTopics: Set[Topic] =
      Set(
        Topic("email", List(subscriber, subscriberTwo)),
        Topic("letter", List(subscriberThree)),
        Topic("telephone", List(subscriberFour))
      )
  }
}
