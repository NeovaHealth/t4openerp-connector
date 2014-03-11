name := "t4openerp-connector"


organization:= "com.tactix4"

version := "1.2-SNAPSHOT"

scalaVersion := "2.10.3"

(sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen

resolvers ++= Seq(
  "Tactix4 Releases" at "http://10.10.160.30:8081/artifactory/libs-release-local",
  "Tactix4 Snapshots" at "http://10.10.160.30:8081/artifactory/libs-snapshot-local",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)


libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.0.5",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "com.typesafe" % "config" % "1.0.2",
  "com.tactix4" %% "t4xmlrpc" % "2.0",
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
  "org.scalatest" %% "scalatest" % "2.0.RC1" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.9" % "test",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.0.5" % "test"
)

initialCommands in console := "import scalaz._, Scalaz._, com.tactix4.t4openerp.connector._,com.tactix4.t4openerp.connector.transport._,com.tactix4.t4openerp.connector.domain._"

initialCommands in console in Test := "import scalaz._, Scalaz._, scalacheck.ScalazProperties._, scalacheck.ScalazArbitrary._,scalacheck.ScalaCheckBinding._,com.tactix4.t4openerp.connector._,com.tactix4.t4openerp.connector.transport._,com.tactix4.t4openerp.connector.domain._"

osgiSettings

OsgiKeys.bundleSymbolicName := "Tactix4 OpenERP-Connector"


OsgiKeys.importPackage ++= Seq(
  "*"
)

OsgiKeys.exportPackage ++= Seq(
    "com.tactix4.t4openerp.connector",
    "com.tactix4.t4openerp.connector.domain",
    "com.tactix4.t4openerp.connector.exception",
    "com.tactix4.t4openerp.connector.field",
    "com.tactix4.t4openerp.connector.transport"
)
