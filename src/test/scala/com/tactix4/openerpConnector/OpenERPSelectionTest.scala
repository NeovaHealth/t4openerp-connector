package com.tactix4.openerpConnector

import com.typesafe.config._
import org.scalatest.concurrent._
import org.scalatest.FunSuite
import scala.concurrent.Await
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.openerpConnector.OpenERPSession._
import com.tactix4.openerpConnector.transport._

/**
 * Created by max@tactix4.com
 * 5/20/13
 */
class OpenERPSelectionTest extends FunSuite with Futures {

  val conf = ConfigFactory.load()

  val username    = conf.getString("openERPServer.username")
  val password    = conf.getString("openERPServer.password")
  val database    = conf.getString("openERPServer.database")
  val openerpHost = conf.getString("openERPServer.hostname")
  val openerpPort = conf.getInt("openERPServer.port")

  val proxy = new OpenERPProxy("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)

  test("write country_id with invalid value") {

    val result = for {
      s <- session
      w <- s.write("res.partner", 1, List("tz" -> "Sepekte/Ubulis"))
    } yield w

    result.onComplete((value: Try[Boolean]) => value match{
      case Failure(f) => fail(f)
      case Success(s) => println("success")
    })
    intercept[OpenERPException]{
      Await.result(result, 3 seconds)
    }

  }

  test("write country_id with valid value") {

    val result = for {
      s <- session
      w <- s.write("res.partner", 1, List("tz" -> "Europe/London"))
    } yield w

    result.onComplete((value: Try[Boolean]) => value match{
      case Failure(f) => fail(f)
      case Success(s) => println("success")
    })
    Await.result(result, 3 seconds)

  }

}
