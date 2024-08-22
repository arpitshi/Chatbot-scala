ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"
libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.11"

lazy val root = (project in file("."))
  .settings(
    name := "chat-Bot"
  )
