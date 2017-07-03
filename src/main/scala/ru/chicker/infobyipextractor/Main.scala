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

import ru.chicker.infobyipextractor.env.ProductionEnv
import ru.chicker.infobyipextractor.service._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object productionEnv extends ProductionEnv

object Main extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def main(args: Array[String]): Unit = {
    try {
      // if the config will not be properly readed then `scopt` will show help usages 
      // and then the program will close.
      
      // : Option[Future[String]] 
      val futCode = for {
        config <- Config.readConfig(args)
        service = new GetInfoByIpServiceImpl(productionEnv)
      } yield service.countryCode(config.ipAddress)

      futCode.foreach { fCode =>
        fCode.onComplete {
          case Success(v) => println(s"country code: $v")
          case Failure(t) => println(s"Unexpected error: ${t.getLocalizedMessage}")
        }

        fCode.onComplete { _ =>
          println("Terminating actor system...")
          productionEnv.actorSystem.terminate()
          Await.ready(productionEnv.actorSystem.whenTerminated, Duration.Inf)
        }

        Await.ready(fCode, 10.seconds)
      }
    } catch {
      case ex: Throwable =>
        println("While starting program an error has been occurred: \n" +
          s"\t${ex.getLocalizedMessage}")
    }
  }
}


