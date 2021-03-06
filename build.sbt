import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.osgi.SbtOsgi.osgiSettings

name := "t4openerp-connector"

organization:= "com.tactix4"

version := "2.0.1"

crossScalaVersions := Seq("2.10.4","2.11.2")

(sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen

libraryDependencies ++= Seq(
  "com.tactix4" %% "t4xmlrpc" % "2.0.2",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.9" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.48" % "test"
)

initialCommands in console := "import scalaz._, Scalaz._, com.tactix4.t4openerp.connector._,com.tactix4.t4openerp.connector.transport._,com.tactix4.t4openerp.connector.domain._"

initialCommands in console in Test := "import scalaz._, Scalaz._, com.tactix4.t4openerp.connector._,com.tactix4.t4openerp.connector.transport._,com.tactix4.t4openerp.connector.domain._"

osgiSettings

OsgiKeys.bundleSymbolicName := "com.tactix4.t4openerp.connector"

OsgiKeys.importPackage ++= Seq(
  "*"
)

pomExtra := (
  <url>https://github.com/Tactix4/t4openerp-connector</url>
    <licenses>
      <license>
        <name>AGPL</name>
        <url>http://www.gnu.org/licenses/agpl-3.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:Tactix4/t4openerp-connector.git</url>
      <connection>scm:git:git@github.com:Tactix4/t4openerp-connector.git</connection>
    </scm>
    <developers>
      <developer>
        <name>Max Worgan</name>
        <email>max@tactix4.com</email>
        <organization>Tactix4 Ltd</organization>
        <organizationUrl>http://www.tactix4.com</organizationUrl>
      </developer>
      </developers>
  )


OsgiKeys.exportPackage ++= Seq(
    "com.tactix4.t4openerp.connector",
    "com.tactix4.t4openerp.connector.domain",
    "com.tactix4.t4openerp.connector.exception",
    "com.tactix4.t4openerp.connector.field",
    "com.tactix4.t4openerp.connector.transport",
    "com.tactix4.t4openerp.connector.codecs"
)
