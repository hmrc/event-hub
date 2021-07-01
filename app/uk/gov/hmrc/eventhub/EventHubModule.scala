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

package uk.gov.hmrc.eventhub

import com.google.inject.{AbstractModule, Provides}
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport
import uk.gov.hmrc.eventhub.model.{Subscriber, Topic}

import javax.inject.{Named, Singleton}


class EventHubModule extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    super.configure()
  }

  @Provides
  @Named("eventTopics")
  @Singleton
  def configTopics(configuration: Configuration): Map[String, Topic] = {
    configuration
      .get[Map[String, List[Subscriber]]]("topics")
      .map { case (k, v) => k -> Topic(k, v) }
  }
}

