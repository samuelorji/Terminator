name := "ControlTerminal"

version := "0.1"

scalaVersion := "2.12.6"



libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"         % "2.6.4",
  "com.typesafe.akka" %% "akka-slf4j"               % "2.6.4",
  "ch.qos.logback"    % "logback-classic"           % "1.2.1",
  "ch.qos.logback"    % "logback-core"              % "1.2.1",
  "com.typesafe.akka" %% "akka-http"                % "10.1.11",
  "com.typesafe.akka" %% "akka-stream"              % "2.6.4",
  "com.typesafe.play" %% "play-json"                % "2.8.1",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.31" % Test,
  "org.scalatest"     %% "scalatest"                % "3.1.1"  % Test

)