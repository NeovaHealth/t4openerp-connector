package com.tactix4.t4openerp.connector

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.tactix4.t4openerp.connector.domain.Domain._
import com.typesafe.config._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 10/01/14
 * Time: 15:55
 * To change this template use File | Settings | File Templates.
 */
class OESessionTestProxy extends FunSuite with LazyLogging with BeforeAndAfterAll {

  val conf = ConfigFactory.load()

  val username    = conf.getString("OEServer.username")
  val password    = conf.getString("OEServer.password")
  val database    = conf.getString("OEServer.database")
  val openerpHost = conf.getString("OEServer.hostname")
  val openerpPort = conf.getInt("OEServer.port")

  val wireMockServer = new WireMockServer(wireMockConfig().port(openerpPort))
  wireMockServer.start()

  val proxy = new OEConnector("http", openerpHost,openerpPort)
  val session = proxy.startSession(username,password,database)

  override def afterAll() {
    wireMockServer.shutdown()
  }


  test("Invalid host returns error"){
    val newSession = new OEConnector("http","myfakehostthatdoesntexist.com",1).startSession(username,password,database)
    newSession.uid.bimap(m => logger.debug(s"Hurrah, we got an error: $m"), _ => fail("Should not be logged in"))
    Await.result(newSession.uid.run, 10 seconds)
  }
  test("Invalid port/port closed returns error"){
    val newSession = new OEConnector("http","localhost",63999).startSession(username,password,database)
    newSession.uid.bimap(m => logger.debug(s"Hurrah, we got an error: $m"), _ => fail("Should not be logged in"))
    Await.result(newSession.uid.run, 10 seconds)
  }
  test("Invalid protocol returns error"){
    try {
      val newSession = new OEConnector("httpz", "localhost", 827342).startSession(username, password, database)
      newSession.uid.bimap(m => logger.debug(s"Hurrah, we got an error: $m"), _ => fail("Should not be logged in"))
      Await.result(newSession.uid.run, 10 seconds)
    } catch {
      case t:Throwable => println(t.getMessage)
    }
  }
  test("Incorrect username returns error"){
    val newSession = new OEConnector("http",openerpHost,openerpPort).startSession(username+"fail",password,database)
    newSession.uid.bimap(
      m =>{
        logger.debug(s"Hurrah, we got an error: $m")
      },
      _ => fail("Should not be logged in"))
    Await.result(newSession.uid.run, 10 seconds)
  }

  test("Incorrect password returns error"){
    val newSession = new OEConnector("http",openerpHost,openerpPort).startSession(username,password+"fail",database)
    newSession.uid.bimap(
      m =>{
        logger.debug(s"Hurrah, we got an error: $m")
      },
      _ => fail("Should not be logged in"))
    Await.result(newSession.uid.run, 10 seconds)
  }

  test("Incorrect database returns error"){
    val newSession = new OEConnector("http",openerpHost,openerpPort).startSession(username,password,database+"fail")
    newSession.uid.bimap(
      m =>{
        logger.debug(s"Hurrah, we got an error: $m")
      },
      _ => fail("Should not be logged in"))
    Await.result(newSession.uid.run, 10 seconds)
  }
  test("Invalid table returns error") {
    val ids = session.search("res.partnerzoid", domain="name" ilike "peter")
    val wait = ids.bimap(
      error => logger.debug(s"Hurrah we got a: $error" ),
      ids   => fail("We should have got an error"))

    Await.result(wait.run, 2 seconds)

  }
  test("invalid domain field returns error") {
    val ids = session.search("res.partner", domain="fieldThatDoesntExist" ilike "peter")

    val wait = ids.bimap(
      error => logger.debug(s"Hurrah we got a: $error" ),
      ids   => fail("We should have got an error")
    )

    Await.result(wait.run, 2 seconds)
  }
  test("Search from res.partner table") {
    val ids = session.search("res.partner", domain="name" ilike "peter")
    val wait = ids.bimap(
      error => fail(s"That didn't work: $error" ),
      ids   => logger.debug(ids.toString))


    Await.result(wait.run, 2 seconds)

  }

  test("Read from res.partner table") {
    val result = session.read("res.partner", List(16))

    val wait = result.bimap(e => fail(s"Something went wrong: $e"), l => logger.debug(l.toString))

    Await.result(wait.run, 2 seconds)
  }


  test("Search and Read from res.partner table") {
    val result = session.searchAndRead("res.partner", "id" === 16)

    val wait = result.bimap(
      e => fail(s"Something went wrong: $e"),
      l => logger.debug("Search and read result: " +l))

    Await.result(wait.run, 2 seconds)
  }



  test("Search and read the res.partner table with a domain and fields") {

    val result =  session.searchAndRead("res.partner", ("email" ilike "info@") OR ("is_company" =/= true), List("category_id"), limit = 10)

    val wait = result.swap.map(f => fail(s"Failed to search and read: $f"))

    Await.result(wait.run, 1 seconds)
  }


  test("Create new partner in res.partner") {

    val result = session.create("res.partner",Map("name" -> "McLovin"))

    val wait = result.swap.map(f => fail(s"Failed to create partner in res.partner: $f"))

    Await.result(wait.run, 10 seconds)
  }

  test("Create new partner bin res.partner with invalid field") {

    val result = session.create("res.partner", Map("namsdf09je" -> "McLovin"))

    val wait = result.map(m => fail("Should have failed for invalid field"))

    Await.result(wait.run, 10 seconds)
  }

  test("Update partner in res.partner") {

    val result = for {
      ids <- session.search("res.partner", "name" === "McLovin")
      r <- session.write("res.partner",ids, Map("name" -> "McLovinUpdated"))
    } yield  r

    val wait = result.swap.map(m => fail(s"Something went wrong: $m"))

    Await.result(wait.run, 3 seconds)
  }

  test("Delete partner") {
    val result = for {
      ids <- session.search("res.partner", "name" ilike "McLovinUpdated")
      r <- session.unlink("res.partner",ids)
    } yield r

    val wait =  result.bimap(m => fail(s"Something went wrong: $m"),(b: Boolean) => logger.debug("Hurrah"))

    Await.result(wait.run, 3 seconds)
  }

  test("Call arbitrary method"){

    val result = session.callMethod("res.partner","read", 1).bimap(
      m => fail(s"Something went wrong: $m"),
      b => logger.debug(s"Hurrah! : $b")
    )

    Await.result(result.run, 3 seconds)
  }
  test("Find Person with fields") {
    val result = session.searchAndRead("res.partner", "name" =/= false AND "email" =/= false AND "id" =/= false, List("email", "id", "name")).bimap(
      m => fail(s"Something went wrong: $m"),
      b => logger.debug(s"Hurrah!: $b")
    )
    Await.result(result.run, 3 seconds)
  }


}
