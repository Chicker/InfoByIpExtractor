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

package ru.chicker.infobyipextractor.env

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import cats.data.Reader
import ru.chicker.infobyipextractor.infoprovider.{
  InfoByIpFreeGeoIpProvider,
  InfoByIpIp2IpProvider,
  InfoByIpProvider
}
import ru.chicker.infobyipextractor.util.{HttpWeb, HttpWebImpl}

import scala.concurrent.{ExecutionContext, Future}

trait ProductionEnv extends Env {
  override def httpWeb: Reader[Env, HttpWeb] = Reader { env =>
    new HttpWebImpl(env)
  }

  override def freeGeoIpProvider: Reader[Env, InfoByIpProvider] = Reader {
    env =>
      new InfoByIpFreeGeoIpProvider(env)
  }

  override def ip2IpProvider: Reader[Env, InfoByIpProvider] = Reader { env =>
    new InfoByIpIp2IpProvider(env)
  }

  def shutdown(): Future[Unit] = {
    httpWeb
      .map(h => {
        log.debug("Shutting down the akka-http pool")
        h.shutdown() andThen {
          case _ =>
            log.debug(
              "The connection pool is shutdown. Terminating actor system..."
            )
            actorSystem.terminate()
        }
      })
      .run(this)
  }

  override val actorSystem: ActorSystem = ActorSystem()

  override val materializer: ActorMaterializer =
    ActorMaterializer()(actorSystem)

  override implicit val executionContext: ExecutionContext =
    actorSystem.dispatcher

  private val log = Logging.getLogger(actorSystem, classOf[ProductionEnv])
}
