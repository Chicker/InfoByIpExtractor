/*
 * Copyright 2017 Vadim Agishev (vadim.agishev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.chicker.infobyipextractor.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer
import ru.chicker.infobyipextractor.env.Env

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class HttpWebImpl(env: Env) extends HttpWeb {
  private val STRICT_ENTITY_TIMEOUT = 999.days

  private implicit val actorSystem = env.actorSystem
  private implicit val materializer = env.materializer
  private implicit val executionContext = env.executionContext
  
  override def getUriAsString(uri: String): Future[String] = {
    val resp = Http().singleRequest(HttpRequest(uri = uri))

    resp flatMap { r =>
      if (r.status == StatusCodes.OK) {

        r.entity.toStrict(STRICT_ENTITY_TIMEOUT).map(_.data.utf8String)
      } else {

        r.entity.discardBytes()
        Future.failed(
          new Exception(s"Remote server at $uri has returned status code: ${r.status.value}"))
      }
    }
  }
}
