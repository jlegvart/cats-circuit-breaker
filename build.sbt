val scala3Version = "3.2.2"

val http4sVersion     = "1.0.0-M39"
val catsCoreVersion   = "2.9.0"
val catsEffectVersion = "3.4.8"
val log4catsVersion   = "2.5.0"
val logbackVersion    = "1.4.6"

lazy val root = project
  .in(file("."))
  .settings(
    name                                   := "cats-circuit-breaker",
    version                                := "0.1.0-SNAPSHOT",
    scalaVersion                           := scala3Version,
    libraryDependencies += "org.typelevel" %% "cats-core"           % catsCoreVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect"         % catsEffectVersion,
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j"      % log4catsVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic"     % logbackVersion,
    libraryDependencies += "org.http4s"    %% "http4s-ember-client" % http4sVersion,
    libraryDependencies += "org.http4s"    %% "http4s-ember-server" % http4sVersion,
    libraryDependencies += "org.http4s"    %% "http4s-dsl"          % http4sVersion,
    libraryDependencies += "org.scalameta" %% "munit"               % "0.7.29" % Test
  )

fork in run := true
