package ru.chicker.infobyipextractor.util

import akka.actor.ActorSystem
import akka.stream.UniformFanInShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, OrElse, Source}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class MergerWithFallbackAndTimeout[T](inputsCount: Int, fallbackValue: T,
                                      timeout: FiniteDuration)
                                     (implicit val actorSystem: ActorSystem) {
  private def graph =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val serviceResultsMerger = builder.add(Merge[T](inputsCount, eagerComplete = false))

      val flows = (0 until inputsCount).map { i =>
        val flow = builder.add(
          Flow[T].recover { case _ =>
            fallbackValue
          })
        flow.out ~> serviceResultsMerger.in(i)
        flow
      }

      val orElse = builder.add(OrElse[T])

      serviceResultsMerger ~> Flow[T].filter(_ != fallbackValue) ~> orElse.in(0)
      Source(List(fallbackValue)) ~> orElse.in(1)

      val withTimeoutMerger = builder.add(Merge[T](2, eagerComplete = true))

      orElse.out ~> withTimeoutMerger.in(0)
      Source.fromFuture[T](
        akka.pattern.after(timeout, actorSystem.scheduler)(
          Future.successful(fallbackValue)
        )(actorSystem.dispatcher)) ~> withTimeoutMerger.in(1)

      UniformFanInShape(withTimeoutMerger.out, flows.map(_.in): _*)
    }


}

object MergerWithFallbackAndTimeout {
  def apply[T](inputsCount: Int, fallbackValue: T,
               timeout: FiniteDuration)
              (implicit actorSystem: ActorSystem) = new
      MergerWithFallbackAndTimeout[T](inputsCount, fallbackValue, timeout).graph
}

