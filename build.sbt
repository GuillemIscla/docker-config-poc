
name := "docker-config-poc"

scalaVersion := "2.13.8"

lazy val internal = (project in file("internal"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "internal",
    version := "0.1",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.1",
    ),
    //dockerBaseImage := "eclipse-temurin:8u312-b07-jdk-focal"
    dockerBaseImage := "openjdk:8-jre",
    dockerExposedPorts ++= Seq(9990, 8888),
    bashScriptConfigLocation := Some("${app_home}/../conf/application.${SERVER_ENV}.ini")
  )

lazy val external = (project in file("external"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "external",
    version := "0.1",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.1",
    ),
    //dockerBaseImage := "eclipse-temurin:8u312-b07-jdk-focal"
    dockerBaseImage := "openjdk:8-jre",
    dockerExposedPorts ++= Seq(9990, 8888),
    bashScriptConfigLocation := Some("/opt/conf/ext/application.external.ini")
  )