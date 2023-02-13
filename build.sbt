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

// ergoscript-simulation dependencies
libraryDependencies ++= Seq(
  ("org.scorexfoundation"         %% "sigma-state"     % "5.0.5").exclude("org.typelevel", "cats-kernel_2.12"),
  "org.typelevel"                 %% "cats-core"       % "2.6.1",
  "com.softwaremill.sttp.client3" %% "circe"           % "3.3.18",
  "com.softwaremill.sttp.client3" %% "okhttp-backend"  % "3.3.18",
  "tf.tofu"                       %% "tofu"            % "0.10.8",
  "tf.tofu"                       %% "derevo-circe"    % "0.13.0",
  "org.scalactic"                 %% "scalactic"       % "3.2.9"   % Test,
  "org.scalatest"                 %% "scalatest"       % "3.2.9"   % Test,
  "org.scalatestplus"             %% "scalacheck-1-15" % "3.2.9.0" % Test,
  "org.scalacheck"                %% "scalacheck"      % "1.15.4"  % Test,
  compilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
  compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
)

// kiosk dependencies
libraryDependencies ++= Seq(
  "io.github.ergoplatform" %% "kiosk" % "1.0",
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