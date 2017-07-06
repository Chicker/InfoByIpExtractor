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

package ru.chicker.infobyipextractor.infoprovider

import ru.chicker.infobyipextractor.env.Env
import ru.chicker.infobyipextractor.util.HttpWeb

import scala.concurrent.{ExecutionContext, Future}

class InfoByIpIp2IpProvider(env: Env) extends InfoByIpProvider {
  private implicit val executionContext = env.executionContext

  override def countryCode(ipAddress: String): Future[String] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    implicit lazy val formats = DefaultFormats

    def extractFn(json: String) =
      (parse(json) \ "countryCode").extract[String].toLowerCase

    val uri = s"http://ip-api.com/json/$ipAddress"

    env.httpWeb
      .map(_.getUriAsString(uri).map { result =>
        extractFn(result).toLowerCase
      })
      .run(env)
  }
}
