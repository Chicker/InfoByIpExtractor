/*
 * Copyright 2017 Vadim Agishev (vadim.agishev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "infobyipextractor"

version := "1.0"

scalaVersion := "2.12.2"

val akkaStreamsV = "2.4.19"
val akkaHttpV    = "10.0.8"
val json4sV      = "3.5.2"
val scalaMockV   = "3.5.0"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++=
  Seq("com.typesafe.akka" %% "akka-stream"                 % akkaStreamsV,
      "com.typesafe.akka" %% "akka-http-core"              % akkaHttpV,
      "org.json4s"        %% "json4s-native"               % json4sV,
      "com.github.scopt"  %% "scopt"                       % "3.6.0",
      "org.typelevel"     %% "cats"                        % "0.9.0",
      "org.scalatest"     %% "scalatest"                   % "3.0.1" % Test,
      "org.scalamock"     %% "scalamock-scalatest-support" % scalaMockV % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"         % akkaStreamsV % Test,
      "com.typesafe.akka" %% "akka-http-testkit"           % akkaHttpV % Test)

mainClass in assembly := Some("ru.chicker.infobyipextractor.Main")
test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _ *) => MergeStrategy.discard
  // for the Akka
  case "reference.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

fork in compile := true

// Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions in ThisBuild ++= Seq("-language:_",
                                   "-deprecation",
                                   "-encoding",
                                   "UTF-8", // yes, this is 2 args
                                   "-feature",
                                   "-unchecked",
                                   "-Xfatal-warnings",
                                   "-Xlint",
                                   "-Yno-adapted-args",
                                   "-Ywarn-dead-code")
