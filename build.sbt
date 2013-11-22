name := "t4openerp-connector"

organization:= "com.tactix4"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq(
  "Tactix4 Artifactory" at "http://10.10.160.30/artifactory/ext-release-local",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
 "Local SBT Repository" at Path.userHome.asFile.toURI.toURL+".ivy2/local"
)


libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.2"

libraryDependencies += "com.tactix4" %% "t4xmlrpc" % "1.0-SNAPSHOT"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.RC1" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9" % "test"

libraryDependencies += "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"

libraryDependencies += "com.typesafe" % "config" % "1.0.2"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

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
