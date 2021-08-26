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

package uk.gov.hmrc.eventhub.controllers

import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.eventhub.service.EventPublisherService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PublishController @Inject()(cc: ControllerComponents, eventPublisherService: EventPublisherService)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def publish(topic: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request
      .body
      .validate[Event]
      .fold(
        errors => Future.successful(BadRequest(Json.obj("Invalid Event payload: " -> JsError.toJson(errors)))),
        event =>
          eventPublisherService.publish(event, topic).map {
            case Right(subscribers) => Created(Json.toJson(PublishResponse(subscribers.map(_.name))))
            case Left(error) =>
              error match {
                case e: DuplicateEvent        => Created(e.message)
                case e: NoEventTopic          => NotFound(e.message)
                case e: NoSubscribersForTopic => Created(e.message)
                case e                        => InternalServerError(e.message)
              }
          }
      )
  }
}
