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
import akka.stream.scaladsl.{GraphDSL, Keep, Merge, Sink, Source}
import cats.data.Reader
import ru.chicker.infobyipextractor.env.Env

import scala.concurrent.Future
import scala.concurrent.duration._

class GetInfoByIpServiceImpl(env: Env) extends GetInfoByIpService {
  private implicit val executionContext = env.executionContext
  
  private val countryCodesByIpProviders = Seq(env.freeGeoIpProvider, env.ip2IpProvider)

  override def countryCode(ipAddress: String, fallbackTimeout: FiniteDuration = 10.seconds): Future[String] = {
    val FALLBACK_COUNTRY_CODE = "lv"

    val g = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val inputsCount = countryCodesByIpProviders.size + 1

      val merge = builder.add(Merge[String](inputsCount, eagerComplete = true))

      for (pIndex <- countryCodesByIpProviders.indices) {

        val infoByIpProvider = countryCodesByIpProviders(pIndex)

        val cCode: Reader[Env, Future[String]] = infoByIpProvider.map(_.countryCode(ipAddress))

        cCode.map { cc =>
          Source.fromFuture(cc) ~> merge.in(pIndex)
        }.run(env)

      }

      Source.fromFuture(
        akka.pattern.after(fallbackTimeout, env.actorSystem.scheduler)(Future.successful(FALLBACK_COUNTRY_CODE))) ~> merge.in(inputsCount - 1)
      
      SourceShape(merge.out)
    })

    g.toMat(Sink.head[String])(Keep.right).run()(env.materializer)
  }
}

//object GetInfoByIpServiceImpl {
//  //  def apply(ipAddress: String): GetInfoByIpService = {
//  //    import scala.concurrent.ExecutionContext.Implicits.global
//  //
//  //    implicit val actorSystem = ActorSystem()
//  //    implicit val materializer = ActorMaterializer()
//  //
//  //    new GetInfoByIpServiceImpl(new HttpWebImpl())
//  //  }
//
//  def apply(): GetInfoByIpServiceImpl = {
//    new GetInfoByIpServiceImpl()
//      with HttpWebComponent
//      with InfoByIpFreeGeoIpProviderComponent
//      with InfoByIpIp2IpProviderModuleComponent
//  }
//
//
////  def countryCode(ipAddress: String): Future[String] = {
////    import scala.concurrent.ExecutionContext.Implicits.global
////    
////    object MySystem extends ActorSystemComponent
////    
//////    implicit val actorSystem = 
//////    implicit val materializer = 
////
////
////
////    val future = service.countryCode(ipAddress)()
////
////    future.onComplete(_ => MySystem.actorSystem.terminate())
////
////    future
////  }
//}
