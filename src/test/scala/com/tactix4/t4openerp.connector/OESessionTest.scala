package com.tactix4.t4openerp.connector

import com.typesafe.config._
import org.scalatest.FunSuite
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.t4openerp.connector.domain.Domain._
import com.tactix4.t4openerp.connector.transport.OEMap
import com.tactix4.t4openerp.connector.codecs.{GeneratedDecodeOETEST, OEDataEncoder, GeneratedDecodeOE, OEDataDecoder}

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 10/01/14
 * Time: 15:55
 * To change this template use File | Settings | File Templates.
 */
class OESessionTest extends FunSuite{

 val conf = ConfigFactory.load()

  val username    = conf.getString("openERPServer.username")
  val password    = conf.getString("openERPServer.password")
  val database    = conf.getString("openERPServer.database")
  val openerpHost = conf.getString("openERPServer.hostname")
  val openerpPort = conf.getInt("openERPServer.port")

  val proxy = new OEConnector("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)

  test("Invalid host returns error"){
    val newSession = new OEConnector("http","myfakehostthatdoesntexist.com",1).startSession(username,password,database)
    newSession.uid.biMap(m => println(s"Hurrah, we got an error: $m"), _ => fail("Should not be logged in"))
    Await.result(newSession.uid.value, 10 seconds)
  }
  test("Invalid port/port closed returns error"){
    val newSession = new OEConnector("http","localhost",63999).startSession(username,password,database)
    newSession.uid.biMap(m => println(s"Hurrah, we got an error: $m"), _ => fail("Should not be logged in"))
    Await.result(newSession.uid.value, 10 seconds)
  }
  test("Invalid protocol returns error"){
    val newSession = new OEConnector("httpz","localhost",63999).startSession(username,password,database)
    newSession.uid.biMap(m => println(s"Hurrah, we got an error: $m"), _ => fail("Should not be logged in"))
    Await.result(newSession.uid.value, 10 seconds)
  }
  test("Incorrect username returns error"){
    val newSession = new OEConnector("http",openerpHost,openerpPort).startSession(username+"fail",password,database)
    newSession.uid.biMap(
      m =>{
        println(s"Hurrah, we got an error: $m")
        newSession.isLoggedIn.map(b => assert(b))
      },
      _ => fail("Should not be logged in"))
    Await.result(newSession.uid.value, 10 seconds)
  }

  test("Incorrect password returns error"){
    val newSession = new OEConnector("http",openerpHost,openerpPort).startSession(username,password+"fail",database)
    newSession.uid.biMap(
      m =>{
        println(s"Hurrah, we got an error: $m")
        newSession.isLoggedIn.map(b => assert(b))
      },
      _ => fail("Should not be logged in"))
    Await.result(newSession.uid.value, 10 seconds)
  }

  test("Incorrect database returns error"){
    val newSession = new OEConnector("http",openerpHost,openerpPort).startSession(username,password,database+"fail")
    newSession.uid.biMap(
      m =>{
        println(s"Hurrah, we got an error: $m")
        newSession.isLoggedIn.map(b => assert(b))
      },
      _ => fail("Should not be logged in"))
    Await.result(newSession.uid.value, 10 seconds)
  }
  test("Invalid table returns error") {
    val ids = session.search("res.partnerzoid", domain="name" ilike "peter")
    val wait = ids.biMap(
      error => println(s"Hurrah we got a: $error" ),
      ids   => fail("We should have got an error"))

    Await.result(wait.value, 2 seconds)

  }
  test("invalid domain field returns error") {
    val ids = session.search("res.partner", domain="fieldThatDoesntExist" ilike "peter")

    val wait = ids.biMap(
      error => println(s"Hurrah we got a: $error" ),
      ids   => fail("We should have got an error")
    )

    Await.result(wait.value, 2 seconds)
  }
  test("Search from res.partner table") {
    val ids = session.search("res.partner", domain="name" ilike "peter")
    val wait = ids.biMap(
      error => fail(s"That didn't work: $error" ),
      ids   => println(ids))


    Await.result(wait.value, 2 seconds)

  }

  test("Read from res.partner table") {
    val result = session.read("res.partner", List(16))

    val wait = result.biMap(e => println(s"Something went wrong: $e"), l => l.head.value.toList foreach println)

    Await.result(wait.value, 2 seconds)
  }


  test("Search and Read from res.partner table") {
    val result = session.searchAndRead("res.partner", "id" === 16)

    val wait = result.biMap(
      e => fail(s"Something went wrong: $e"),
      l => println("Search and read result: " +l))

    Await.result(wait.value, 2 seconds)
  }



  test("Search and read the res.partner table with a domain and fields") {

    val result =  session.searchAndRead("res.partner", ("email" ilike "info@") OR (("is_company" =/= true)), List("category_id"), limit = 10)

    val wait = result.failMap(f => fail(s"Failed to search and read: $f"))

    Await.result(wait.value, 1 seconds)
  }


  test("Create new partner in res.partner") {

    val result = session.create("res.partner",Map("name" -> "McLovin"))

    val wait = result.failMap(f => fail(s"Failed to create partner in res.partner: $f"))

    Await.result(wait.value, 10 seconds)
  }

  test("Create new partner bin res.partner with invalid field") {

    val result = session.create("res.partner", Map("namsdf09je" -> "McLovin"))

    val wait = result.map(m => fail("Should have failed for invalid field"))

    Await.result(wait.value, 10 seconds)
  }

  test("Update partner in res.partner") {

   val result = for {
       ids <- session.search("res.partner", "name" === "McLovin")
      r <- session.write("res.partner",ids, Map("name" -> "McLovinUpdated"))
    } yield  r

    val wait = result.failMap(m => fail(s"Something went wrong: $m"))

    Await.result(wait.value, 3 seconds)
  }

  test("Delete partner") {
    val result = for {
      ids <- session.search("res.partner", "name" ilike "McLovinUpdated")
      r <- session.unlink("res.partner",ids)
    } yield r

    val wait =  result.biMap(m => fail(s"Something went wrong: $m"),(b: Boolean) => println("Hurrah"))

    Await.result(wait.value, 3 seconds)
  }

  test("Call arbitrary method"){

    val result = session.callMethod("res.partner","read", 1).biMap(
      m => fail(s"Something went wrong: $m"),
      b => println("Hurrah!")
    )

    Await.result(result.value, 3 seconds)
  }
  test("Find Person with fields") {
    val result = session.searchAndRead("res.partner", "name" =/= false AND "city" =/= false, List("name"), limit = 1).biMap(
      m => fail(s"Something went wrong: $m"),
      b => println(s"Hurrah!: $b")
    )
    Await.result(result.value, 3 seconds)
  }

  test("Test decoding"){
    import GeneratedDecodeOE._
    import GeneratedDecodeOETEST._
    case class Person(id:Id, name:String, city:String, function:String)

    implicit val DecodePerson: OEDataDecoder[Person] =
        decode4M(Person(_:Id, _:String,_:String, _:String))("id", "name", "city","function")

    implicit val personEncode :OEDataEncoder[Person] =
        encode4M((p:Person) => (p.id,p.name,p.city,p.function))("id","name","city","function")


    val p = Person(10,"James","London","Chief")
    val e = p.encode


    val result = session.searchAndRead("res.partner","name" ilike "James", List("name", "city","function")).biMap(
      (message: ErrorMessage) => fail(message),
      (records: List[OEMap]) => println(records.map( m => m.decodeAs[Person]))
    )

    Await.result(result.value, 3 seconds)

  }

}
