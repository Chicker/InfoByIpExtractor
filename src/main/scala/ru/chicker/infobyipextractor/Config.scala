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

package ru.chicker.infobyipextractor

case class Config(ipAddress: String = "")

object Config {
  private val APP_NAME = "Information by IP-address extractor"

  def readConfig(args: Array[String]): Option[Config] = {
    val cliParser = new scopt.OptionParser[Config](APP_NAME) {
      head(APP_NAME)

      arg[String]("<ip-address>")
        .action((ipAddress, c) => c.copy(ipAddress = ipAddress))
    }

    cliParser.parse(args, Config())
  }
}
