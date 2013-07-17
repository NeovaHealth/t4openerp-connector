name := "openerpxmlrpc"

version := "1.0"

scalaVersion := "2.10.1"

resolvers ++= Seq(
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)

resolvers += "Local SBT Repository" at Path.userHome.asFile.toURI.toURL+".ivy2/local"


libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.0"

libraryDependencies += "simplexmlrpc" %% "simplexmlrpc" % "1.0-SNAPSHOT"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"

libraryDependencies += "com.typesafe" %% "scalalogging-log4j" % "1.0.1"

libraryDependencies += "com.typesafe" % "config" % "1.0.2"