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

package uk.gov.hmrc.eventhub.subscription

import org.scalacheck.Gen
import play.api.libs.json.{ JsArray, JsObject, JsString, JsValue }
import uk.gov.hmrc.eventhub.model.Event

import java.time.format.DateTimeFormatter
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset }

object Generators {
  val fencingSubjectsGen: Gen[String] = Gen.oneOf(
    Seq(
      "bounces",
      "aids",
      "appel",
      "attack",
      "beat",
      "bib",
      "bind",
      "blade",
      "bout"
    )
  )

  val fencingGroupsGen: Gen[String] = Gen.oneOf(
    Seq(
      "epee",
      "foil",
      "sabre"
    )
  )

  val StartYear = 2000
  val EndYear = 2200
  val DayMonth = 1
  val lowerBound: Instant = LocalDate.of(StartYear, DayMonth, DayMonth).atStartOfDay(ZoneOffset.UTC).toInstant
  val upperBound: Instant = LocalDate.of(EndYear, DayMonth, DayMonth).atStartOfDay(ZoneOffset.UTC).toInstant

  val instantGen: Gen[Instant] = for {
    second <- Gen.choose(min = lowerBound.getEpochSecond, max = upperBound.getEpochSecond)
    nano   <- Gen.choose(min = lowerBound.getNano, max = upperBound.getNano)
  } yield Instant.ofEpochSecond(second, nano.toLong)
  val localDateTimeGen: Gen[LocalDateTime] = instantGen.map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))
  val localDateGen: Gen[LocalDate] = localDateTimeGen.map(_.toLocalDate)

  val jsValueGen: Gen[JsValue] = ChannelPreferences.bouncedEmailJsonGen

  val eventGen: Gen[Event] = for {
    eventId   <- Gen.uuid
    subject   <- fencingSubjectsGen
    groupId   <- fencingGroupsGen
    timeStamp <- localDateTimeGen
    event     <- jsValueGen
  } yield Event(eventId, subject, groupId, timeStamp, event)

  val emailGen: Gen[String] = for {
    emailUser   <- Gen.oneOf("jack", "john", "jake", "jasmine", "jane", "josephine")
    emailDomain <- Gen.oneOf("gmail.com", "hotmail.com", "sky.com", "yahoo.com", "aol.com")
  } yield s"$emailUser@$emailDomain"

  object ChannelPreferences {
    val bouncedEmailJsonGen: Gen[JsObject] = for {
      event     <- Gen.const("failed")
      email     <- emailGen
      detected  <- localDateTimeGen
      reason    <- Gen.oneOf("Not delivering to previously bounced address", "Virgin bounce")
      enrolment <- Gen.const("HMRC-MTD-VAT~VRN~GB123456789")
    } yield
      JsObject(
        Seq(
          "event"     -> JsString(event),
          "email"     -> JsString(email),
          "detected"  -> JsString(detected.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
          "reason"    -> JsString(reason),
          "enrolment" -> JsString(enrolment)
        )
      )
  }
}
