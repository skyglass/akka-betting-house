val AkkaVersion = "2.6.20"
val AlpakkaKafkaVersion = "3.0.1"
val LogbackVersion = "1.2.3"
val ScalaVersion = "2.13.15"
val AkkaManagementVersion = "1.1.4"
val AkkaProjectionVersion = "1.2.2"
val ScalikeJdbcVersion = "3.5.0"
val AkkaHttpVersion = "10.2.9"
val AkkaGRPC = "2.0.0"
val ScalaTest = "3.1.4"
val JacksonVersion = "2.11.4" 
val AkkaStreamAlpakka = "4.0.0"
val AkkaStreamKafka = "3.0.1"

lazy val `betting-house` = project
    .in(file("betting-house"))
    .enablePlugins(AkkaGrpcPlugin, JavaAppPackaging, DockerPlugin)
    .settings(
      version := "0.1.0-SNAPSHOT",
      dockerUsername := Some("bettinghouse.azurecr.io"), // assumes docker.io by default
      scalafmtOnCompile := true,
      Compile / mainClass := Some("example.betting.Main"), 
      scalaVersion := ScalaVersion,
      dockerExposedPorts := Seq(8558, 2552, 9000, 9001, 9002, 9003),
      dockerBaseImage := "adoptopenjdk:11-jre-hotspot",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,         
        "com.typesafe.akka" %% "akka-cluster" % AkkaVersion, //needed?
        "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
        "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
        "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
        "org.scalatest" %% "scalatest" % ScalaTest % Test, 
        "org.scalikejdbc" %% "scalikejdbc"       % ScalikeJdbcVersion,
        "org.scalikejdbc" %% "scalikejdbc-config" % ScalikeJdbcVersion,
        "org.postgresql" % "postgresql" % "42.2.18",
        "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion,
        "com.typesafe.akka" %% "akka-stream-kafka-cluster-sharding" % AlpakkaKafkaVersion,
        "org.apache.kafka" % "kafka-clients" % "3.8.0",
        "com.lightbend.akka" %% "akka-projection-core" % AkkaProjectionVersion,
        "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
        "com.lightbend.akka" %% "akka-projection-jdbc" % AkkaProjectionVersion
      ))



ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger