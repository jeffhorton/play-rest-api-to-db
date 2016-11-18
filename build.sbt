import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

scalaVersion := "2.11.8"



libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.14"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.2.2" % Test

libraryDependencies += "com.typesafe.play" %% "play-slick" % "2.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0"

//libraryDependencies += "org.xerial"          %   "sqlite-jdbc"   % "3.7.2"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34" 

// jeff adding later for anorm instead of slick
//libraryDependencies ++= Seq(
//  jdbc,
//  "com.typesafe.play" %% "anorm" % "2.5.0"
//)

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """play-rest-api""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    paradoxProperties += ("download_url" -> "https://example.lightbend.com/v1/download/play-rest-api")
  )
