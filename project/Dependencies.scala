import sbt.*

object Dependencies {
  private val akkaVersion     = "2.6.21"
  private val akkaHttpVersion = "10.2.10"

  val loggingDependencies = Seq(
    "com.typesafe.akka"          %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"              % "logback-classic" % "1.4.11",
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.5"
  )

  val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"                % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence"           % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-typed"     % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query"     % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.107",
    "com.typesafe.akka" %% "akka-discovery"             % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools"         % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-http"                  % akkaHttpVersion,
    "ch.megard"         %% "akka-http-cors"             % "1.2.0",
    "com.typesafe.akka" %% "akka-stream-kafka"          % "3.0.1"
  )

  val alpkakkaDependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-file" % "4.0.0"
  )

  val jsonDependencies = Seq(
    "io.circe"          %% "circe-core"           % "0.14.6",
    "io.circe"          %% "circe-generic"        % "0.14.6",
    "io.circe"          %% "circe-generic-extras" % "0.14.3",
    "io.circe"          %% "circe-parser"         % "0.14.6",
    "io.circe"          %% "circe-optics"         % "0.14.1",
    "de.heikoseeberger" %% "akka-http-circe"      % "1.39.2"
  )

  val mongoDependencies = Seq(
    "org.reactivemongo" %% "reactivemongo"            % "1.0.10",
    "org.reactivemongo" %% "reactivemongo-akkastream" % "1.0.10"
  )

  val testDependencies = Seq(
    "org.scalatest"      %% "scalatest"           % "3.2.17"        % "test",
    "com.typesafe.akka"  %% "akka-http-testkit"   % "10.2.9"        % "test",
    "com.typesafe.akka"  %% "akka-stream-testkit" % "2.6.21"        % "test",
    "org.mockito"        %% "mockito-scala"       % "1.17.29"       % "test",
    "org.testcontainers"  % "testcontainers"      % "1.19.3"        % "test",
    "org.testcontainers"  % "mongodb"             % "1.19.3"        % "test",
    ("com.typesafe.akka" %% "akka-http-xml"       % akkaHttpVersion % "test").exclude("org.scala-lang.modules", "scala-xml")
  )

  val all: Seq[ModuleID] = Seq(
    loggingDependencies,
    akkaDependencies,
    alpkakkaDependencies,
    mongoDependencies,
    jsonDependencies,
    testDependencies
  ).flatten

  val resolvers: Seq[MavenRepository] = Seq(
    "Secured Central Repository".at("https://repo1.maven.org/maven2")
  )
}
