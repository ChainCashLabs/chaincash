import sbt.ExclusionRule
import sbt.Keys.fullClasspath

name := "chaincash"

version := "0.2"
organization := "org.ergoplatform"
scalaVersion := "3.2.1"


resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Bintray" at "https://jcenter.bintray.com/", //for org.ethereum % leveldbjni-all
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "https://dl.bintray.com/typesafe/maven-releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= Seq()


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