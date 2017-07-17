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

package ru.chicker.infobyipextractor.service

import akka.stream.SourceShape
import akka.stream.scaladsl.{GraphDSL, Keep, Sink, Source}
import cats.data.Reader
import ru.chicker.infobyipextractor.env.Env
import ru.chicker.infobyipextractor.util.MergerWithFallbackAndTimeout

import scala.concurrent.Future
import scala.concurrent.duration._


private[this] class GetInfoByIpServiceImpl(env: Env)
  extends GetInfoByIpService {

  private implicit val actorSystem = env.actorSystem
//  private implicit val executionContext = env.executionContext

  private val countryCodesByIpProviders =
    Seq(env.freeGeoIpProvider, env.ip2IpProvider)

  override def countryCode(
                            ipAddress: String,
                            fallbackTimeout: FiniteDuration = 10.seconds
                          ): Future[String] = {

    val FALLBACK_COUNTRY_CODE = "lv"

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val futCompleter = builder.add(MergerWithFallbackAndTimeout(
        countryCodesByIpProviders.length,
        FALLBACK_COUNTRY_CODE, fallbackTimeout))

      for (pIndex <- countryCodesByIpProviders.indices) {

        val infoByIpProvider = countryCodesByIpProviders(pIndex)

        val cCode: Reader[Env, Future[String]] =
          infoByIpProvider.map(_.countryCode(ipAddress))

        cCode.map { cc =>
          Source.fromFuture(cc) ~> futCompleter.in(pIndex)
        }.run(env)

      }

      SourceShape(futCompleter.out)
    }).toMat(Sink.head[String])(Keep.right).run()(env.materializer)
  }
}

object GetInfoByIpServiceImpl {

  def apply(env: Env): GetInfoByIpService = {
    new GetInfoByIpServiceImpl(env)
  }
}
