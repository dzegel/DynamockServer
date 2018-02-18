organization := "com.dzegel"

homepage := Some(url("https://github.com/dzegel/DynamockServer"))

val applicationName = "DynamockServer"
val applicationVersion = "2.0.1"

name := applicationName

startYear := Some(2017)

version := applicationVersion

scalaVersion := "2.12.3"

assemblyOutputPath in assembly := file(s"releases/$applicationName-$applicationVersion.jar")

mainClass in assembly := Some("com.dzegel.DynamockServer.server.DynamockServerMain")

lazy val versions = new {
  val finatra = "17.12.0"
}

libraryDependencies ++= Seq(

  "com.twitter" %% "finatra-http" % versions.finatra,
  "com.twitter" %% "finatra-httpclient" % versions.finatra,
  "com.twitter" %% "inject-request-scope" % versions.finatra,

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.json4s" %% "json4s-native" % "3.6.0-M2",

  "com.twitter" %% "finatra-http" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "finatra-jackson" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",

  "com.twitter" %% "finatra-http" % versions.finatra % "test",
  "com.twitter" %% "finatra-jackson" % versions.finatra % "test",
  "com.twitter" %% "inject-server" % versions.finatra % "test",
  "com.twitter" %% "inject-app" % versions.finatra % "test",
  "com.twitter" %% "inject-core" % versions.finatra % "test",
  "com.twitter" %% "inject-modules" % versions.finatra % "test",

  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "com.google.inject.extensions" % "guice-testlib" % "4.1.0" % "test"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
