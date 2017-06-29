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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.Reader
import ru.chicker.infobyipextractor.env.{Env, Production}
import ru.chicker.infobyipextractor.infoprovider.{InfoByIpFreeGeoIpProvider, InfoByIpIp2IpProvider, InfoByIpProvider}
import ru.chicker.infobyipextractor.service._
import ru.chicker.infobyipextractor.util.{HttpWeb, HttpWebImpl}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object productionEnv extends Env {
  override implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def httpWeb: Reader[Env, HttpWeb] = Reader { env =>
    new HttpWebImpl(actorSystem, materializer)
  }

  override def freeGeoIpProviderH: Reader[HttpWeb, InfoByIpProvider] = Reader { h =>
    new InfoByIpFreeGeoIpProvider(h)
  }

  override def ip2IpProviderH: Reader[HttpWeb, InfoByIpProvider] = Reader { h =>
    new InfoByIpIp2IpProvider(h)
  }

  override def actorSystem: ActorSystem = ActorSystem()

  override def materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
}

object Main extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  override def main(args: Array[String]): Unit = {
    try {
      // if the config will not be properly readed then `scopt` will show help usages 
      // and then the program will close.
      val config = Config.readConfig(args)

      config foreach { cfg =>
        val service = new GetInfoByIpServiceImpl(productionEnv)
        
        val futCountryCode = service.countryCode(cfg.ipAddress)

        futCountryCode.onComplete {
          case Success(v) => println(s"country code: $v")
          case Failure(t) => println(s"Unexpected error: ${t.getLocalizedMessage}")
        }
        
        futCountryCode.onComplete { _ =>
          println("Terminating actor system...")
          productionEnv.actorSystem.terminate()
          Await.ready(productionEnv.actorSystem.whenTerminated, Duration.Inf)
        }

        Await.ready(futCountryCode, 10.seconds)

        //        scala.sys.addShutdownHook {
        //          logger.info("Terminating...")
        //          actorSystem.terminate()
        //          Await.result(actorSystem.whenTerminated, 30 seconds)
        //          logger.info("Terminated... Bye")
        //        }
        
      }
    } catch {
      case ex: Throwable =>
        println("While starting program an error has been occurred: \n" +
          s"\t${ex.getLocalizedMessage}")
    }
  }
}


