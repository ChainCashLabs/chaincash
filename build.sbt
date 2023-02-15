import sbt.ExclusionRule
import sbt.Keys.fullClasspath

name := "chaincash"

version := "0.2"
organization := "org.ergoplatform"
scalaVersion := "2.12.17"

unmanagedClasspath in Compile += baseDirectory.value / "contracts"

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Bintray" at "https://jcenter.bintray.com/", //for org.ethereum % leveldbjni-all
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "https://dl.bintray.com/typesafe/maven-releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Nexus Releases" at "https://s01.oss.sonatype.org/content/repositories/releases",
  "Nexus Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies += "io.github.getblok-io" %% "getblok_plasma" % "1.0.1"


// kiosk dependencies
libraryDependencies ++= Seq(
  ("io.github.ergoplatform" %% "kiosk" % "1.0").exclude("org.ergoplatform", "ergo-appkit_2.12"),
  // https://mvnrepository.com/artifact/org.ergoplatform/ergo-appkit
  "org.ergoplatform" %% "ergo-appkit" % "5.0.0",
  "com.squareup.okhttp3" % "mockwebserver" % "3.14.9",
  "org.scalatest" %% "scalatest" % "3.0.8" ,
  "org.scalacheck" %% "scalacheck" % "1.14.+" ,
  "org.mockito" % "mockito-core" % "2.23.4"
)

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"

assembly / assemblyMergeStrategy := {
  case "logback.xml" => MergeStrategy.first
  case PathList("reference.conf") => MergeStrategy.concat
  case manifest if manifest.contains("MANIFEST.MF") => MergeStrategy.discard
  case manifest if manifest.contains("module-info.class") => MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") => MergeStrategy.concat
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

assembly / mainClass := Some("play.core.server.ProdServerStart")