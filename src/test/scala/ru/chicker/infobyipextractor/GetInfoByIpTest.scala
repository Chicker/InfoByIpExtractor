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
import cats.data.Reader
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import ru.chicker.infobyipextractor.env.{Env, ProductionEnv}
import ru.chicker.infobyipextractor.infoprovider.{InfoByIpFreeGeoIpProvider, InfoByIpIp2IpProvider, InfoByIpProvider}
import ru.chicker.infobyipextractor.service._
import ru.chicker.infobyipextractor.util.HttpWeb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class GetInfoByIpTest extends FunSpec with Matchers with MockFactory {
  it("should return country code for known ip-address. Using IpByIp") {
    object testEnv extends ProductionEnv {
      // language=JSON
      val stubResult = "{\"as\":\"AS24940 Hetzner Online GmbH\",\"city\":\"Nuremberg\",\"country\":\"Germany\",\"countryCode\":\"DE\"}"

      override def httpWeb = Reader[Env, HttpWeb] { _ =>
        val mockHttpWeb = mock[HttpWeb]

        (mockHttpWeb.getUriAsString(_: String))
          .expects(*).returning(Future.successful(stubResult)).anyNumberOfTimes()

        mockHttpWeb
      }
    }

    val testIpAddress = "78.47.232.67"
    
    val service = new InfoByIpIp2IpProvider(testEnv)

    val futCountryCode = service.countryCode(testIpAddress)

    Await.result(futCountryCode, 20.seconds) shouldBe "de"
  }

  it("should return country code for known ip-address. Using freegeoip") {

    object testEnv extends ProductionEnv {
      // language=JSON
      val stubResult = "{\"ip\":\"78.47.232.67\",\"country_code\":\"DE\",\"country_name\":\"Germany\"}"

      override def httpWeb = Reader[Env, HttpWeb] { _ =>
        val mockHttpWeb = mock[HttpWeb]

        (mockHttpWeb.getUriAsString(_: String))
          .expects(*).returning(Future.successful(stubResult)).anyNumberOfTimes()

        mockHttpWeb
      }
    }

    val testIpAddress = "78.47.232.67"

    val service = new InfoByIpFreeGeoIpProvider(testEnv)

    val futCountryCode = service.countryCode(testIpAddress)

    Await.result(futCountryCode, 20.seconds) shouldBe "de"
  }

  def futureFailedAfter(duration: FiniteDuration)(implicit actorSystem: ActorSystem) =
    akka.pattern.after(duration, actorSystem.scheduler)(Future.failed(new Exception))


  it("should return fall-back country code if all of the services are not accessible" +
    "within 3 seconds") {

    object testEnv extends ProductionEnv {
      override def freeGeoIpProvider: Reader[Env, InfoByIpProvider] =
        Reader { _ =>
          val m = mock[InfoByIpProvider]

          (m.countryCode(_: String))
            .expects(*)
            .returns(futureFailedAfter(20.seconds)(actorSystem))
          m
        }

      override def ip2IpProvider: Reader[Env, InfoByIpProvider] =
        Reader { _ =>
          val m = mock[InfoByIpProvider]

          (m.countryCode(_: String))
            .expects(*)
            .returns(futureFailedAfter(20.seconds)(actorSystem))
          m
        }
    }

    val testIpAddress = "78.47.232.67"

    val service = new GetInfoByIpServiceImpl(testEnv)

    val futCountryCode = service.countryCode(testIpAddress, fallbackTimeout = 3.seconds)

    val FALLBACK_COUNTRY_CODE = "lv"
    Await.result(futCountryCode, 4.seconds) shouldBe FALLBACK_COUNTRY_CODE
  }

//    it("integration test") {
//      val testIpAddress = "78.47.232.67"
//      val service = new GetInfoByIpServiceImpl(productionEnv)
//
//      val futCountryCode = service.countryCode(testIpAddress, fallbackTimeout = 3.seconds)
//
//      Await.result(futCountryCode, 4.seconds) shouldBe "de"
//    }
}
