name    := "mp-scala-rankomat"
version := "0.1"

enablePlugins(SbtNativePackager)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

val baseSettings = Seq(
  organization := "com.mp",
  scalaVersion := "2.13.15",
  resolvers ++= Dependencies.resolvers,
  libraryDependencies ++= Dependencies.all,
  scalacOptions ++= CompilerOpts.scalacOptions,
  Test / parallelExecution               := false,
  Compile / doc / sources                := Nil,
  Compile / packageDoc / publishArtifact := false
)

val dockerSettings = Seq(
  dockerBaseImage     := "eclipse-temurin:21-jre",
  dockerUsername      := Some("michalpieczonka4"),
  dockerExposedPorts  := Seq(8080),
  Docker / daemonUser := "root"
)


lazy val root = project
  .in(file("."))
  .settings(baseSettings)
  .settings(dockerSettings)
