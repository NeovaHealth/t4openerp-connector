package com.tactix4.openerpConnector

import com.typesafe.config._
import org.scalatest.concurrent._
import org.scalatest.FunSuite
import scala.concurrent.{Future, Await}
import scala.concurrent.Future._
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.openerpConnector.domain.Domain._

import com.tactix4.openerpConnector.transport._
import OpenERPSession._
/**
 * Created by max@tactix4.com
 * 5/20/13
 */
class OpenERPMany2ManyTest extends FunSuite with Futures {

  val conf = ConfigFactory.load()

  val username    = conf.getString("openERPServer.username")
  val password    = conf.getString("openERPServer.password")
  val database    = conf.getString("openERPServer.database")
  val openerpHost = conf.getString("openERPServer.hostname")
  val openerpPort = conf.getInt("openERPServer.port")

  val proxy = new OpenERPProxy("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)

  test("Query category_id field in res.partner") {
    val result = for {
      s <- session
      r <- s.searchAndRead(model="res.partner", fields="category_id")
    } yield r

    result.onComplete((value: Try[List[List[(String, Any)]]]) => value match{
      case Success(s) => println(s)
      case Failure(f) => fail(f)
    })

    Await.result(result, 2 seconds)

  }

  test("Query only non-empty category_id field in res.partner") {
    val result = for {
      s <- session
      r <- s.searchAndRead("res.partner", "category_id" =/= false, "category_id")
    } yield r

    result.onComplete((value: Try[List[List[(String, Any)]]]) => value match{
      case Success(s) => println(s)
      case Failure(f) => fail(f)
    })

    Await.result(result, 2 seconds)

  }

  test("Update first non-empty category_id field in res.partner") {

    val result = for {
      s <- session
      ids <- s.search("res.partner", "category_id" =/= false, limit=1)
      //set values to some value
      update <- s.write("res.partner",ids, List("category_id"-> List(10,13)))
      //read starting values
      startVal <- s.read("res.partner", ids, List("category_id"))
      //update values
      update <- s.write("res.partner",ids, List("category_id"-> List(10,8)))
      //read values again to check they've been changed
      check <- s.read("res.partner", ids, List("category_id"))

    } yield {println(startVal); check != startVal}

    result.onComplete((value: Try[Boolean]) => value match {
      case Success(s) => s match {
        case true   => println("Successful write")
        case false  => fail("Unsuccessful write")
      }
      case Failure(f) => fail(f)
    })

    Await.result(result, 2 seconds)
  }

  test("Fail on Update category_id with invalid value") {
    val result = for {
      s <- session
      ids <- s.search("res.partner", "category_id" =/= false, limit=1)
      failwrite <- s.write("res.partner", ids, List("category_id" -> "Some value that doesn't make sense"))
    } yield failwrite
    intercept[OpenERPException]{
      Await.result(result, 2 seconds)
    }
  }


}
