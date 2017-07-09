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

import cats.data.EitherT
import cats.implicits._
import ru.chicker.infobyipextractor.env.ProductionEnv
import ru.chicker.infobyipextractor.service._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

import akka.event.Logging

object productionEnv extends ProductionEnv

object Main extends App {

  import productionEnv.executionContext

  val log = Logging.getLogger(productionEnv.actorSystem, Main.getClass)

  try {
    val code = getCountryCode(args)

    code.value.onComplete {
      case Success(res) =>
        res match {
          case Left(e) =>
            log.error("Error occured when extracting country code: {}", e)
          case Right(v) =>
            println(s"country code: $v")
        }
      case Failure(v) => throw v
    }

    Await.ready(code.value.flatMap(_ => productionEnv.shutdown()),
                Duration.Inf)

  } catch {
    case t: Throwable =>
      log.error("While executing program an error has been occurred: {}",
                t.getLocalizedMessage)
  }

  def getCountryCode(args: Array[String]): EitherT[Future, AppError, String] = {
    val eithConfig =
      EitherT.fromEither[Future](
        Config.readConfig(args).toRight(AppError.CliArgumentsParsingError())
      )

    for {
      config <- eithConfig
      service = GetInfoByIpServiceImpl(productionEnv)
      out <- EitherT.right(service.countryCode(config.ipAddress))
    } yield out
  }
}
