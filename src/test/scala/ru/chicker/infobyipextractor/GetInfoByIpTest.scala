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
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import ru.chicker.infobyipextractor.env.Env
import ru.chicker.infobyipextractor.infoprovider.{InfoByIpFreeGeoIpProvider, InfoByIpIp2IpProvider, InfoByIpProvider}
import ru.chicker.infobyipextractor.service._
import ru.chicker.infobyipextractor.util.HttpWeb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


class GetInfoByIpTest extends FunSpec with Matchers with MockFactory {

  //  private implicit val actorSystem = ActorSystem()
  //  private implicit val materializer = ActorMaterializer()

  trait TestEnv extends Env {


    override implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

    override def httpWeb: Reader[Env, HttpWeb] = Reader { _ => mock[HttpWeb] }

    //    override def httpWeb: HttpWeb = mock[HttpWeb]

    override def freeGeoIpProviderH: Reader[HttpWeb, InfoByIpProvider] =
      Reader { _ =>
        val m = mock[InfoByIpProvider]

        (m.countryCode(_: String))
          .expects(*)
          .returns(futureFailedAfter(20.seconds)(actorSystem))
        m
      }

    override def ip2IpProviderH: Reader[HttpWeb, InfoByIpProvider] =
      Reader { _ =>
        val m = mock[InfoByIpProvider]

        (m.countryCode(_: String))
          .expects(*)
          .returns(futureFailedAfter(20.seconds)(actorSystem))
        m
      }

    override def actorSystem: ActorSystem = ActorSystem()

    override def materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
  }


  it("should return country code for known ip-address. Using IpByIp") {
    val mockHttpWeb = mock[HttpWeb]
    val testIpAddress = "78.47.232.67"

    val service = new InfoByIpIp2IpProvider(mockHttpWeb)

    // language=JSON
    val stubResult = "{\"as\":\"AS24940 Hetzner Online GmbH\",\"city\":\"Nuremberg\",\"country\":\"Germany\",\"countryCode\":\"DE\"}"

    (mockHttpWeb.getUriAsString(_: String))
      .expects(*).returning(Future.successful(stubResult))

    val futCountryCode = service.countryCode(testIpAddress)

    Await.result(futCountryCode, 20.seconds) shouldBe "de"
  }

  it("should return country code for known ip-address. Using freegeoip") {
    val mockHttpWeb = mock[HttpWeb]
    val testIpAddress = "78.47.232.67"

    val service = new InfoByIpFreeGeoIpProvider(mockHttpWeb)
    // language=JSON
    val stubResult = "{\"ip\":\"78.47.232.67\",\"country_code\":\"DE\",\"country_name\":\"Germany\"}"

    (mockHttpWeb.getUriAsString(_: String))
      .expects(*).returning(Future.successful(stubResult))

    val futCountryCode = service.countryCode(testIpAddress)

    Await.result(futCountryCode, 20.seconds) shouldBe "de"
  }

  def readerCompose[A, B, C](r1: Reader[C, A], r2: Reader[A, B]): Reader[C, B] = {
    Reader { c =>
      r2.run(r1.run(c))
    }
  }

  def futureFailedAfter(duration: FiniteDuration)(implicit actorSystem: ActorSystem) =
    akka.pattern.after(duration, actorSystem.scheduler)(Future.failed(new Exception))


  it("should return fall-back country code if all of the services are not accessible" +
    "within 3 seconds") {

    val testIpAddress = "78.47.232.67"

    val service = new GetInfoByIpServiceImpl() with TestEnv

    val futCountryCode = service.countryCode(testIpAddress, fallbackTimeout = 3.seconds)

    val FALLBACK_COUNTRY_CODE = "lv"
    Await.result(futCountryCode, 4.seconds) shouldBe FALLBACK_COUNTRY_CODE
  }

  //    it("integration test") {
  //      val testIpAddress = "78.47.232.67"
  //      val service = new GetInfoByIpServiceImpl() with Production
  //
  //      val futCountryCode = service.countryCode(testIpAddress, fallbackTimeout = 3.seconds)
  //
  //      Await.result(futCountryCode, 4.seconds) shouldBe "de"
  //    }
}
