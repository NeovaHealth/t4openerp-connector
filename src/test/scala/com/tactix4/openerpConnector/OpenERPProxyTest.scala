package com.tactix4.openerpConnector
import com.typesafe.config._
import org.scalatest.concurrent._
import org.scalatest.FunSuite
import scala.concurrent.{Promise, Await}
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.openerpConnector.domain.Domain._
import com.tactix4.openerpConnector.field.FieldType._
import OpenERPSession._
import com.tactix4.openerpConnector.transport._
import com.tactix4.openerpConnector.field.{FieldType, Field}
/**
 * Created by max@tactix4.com
 * 5/20/13
 */
class OpenERPProxyTest extends FunSuite with Futures {

  val conf = ConfigFactory.load()

  val username    = conf.getString("openERPServer.username")
  val password    = conf.getString("openERPServer.password")
  val database    = conf.getString("openERPServer.database")
  val openerpHost = conf.getString("openERPServer.hostname")
  val openerpPort = conf.getInt("openERPServer.port")

  val proxy = new OpenERPProxy("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)

  test("login to openerp host") {
    session.onComplete((value: Try[OpenERPSession]) => value match{
      case Failure(f) => fail(f)
      case Success(s) => println("logged in with uid: " + s.uid)
    })
    Await.result(session, 1 second)

  }


  test("Read from res.partner table") {
    val ids = for {
      s <- session
      i <-  s.read("res.partner",1)
    } yield i

    ids.onComplete((value: Try[List[List[(String, Any)]]]) => value match {
      case Success(s) => s.foreach(_ foreach println)
      case Failure(f) => fail(f)
    })

    Await.result(ids, 1 second)

  }


  test("Fail to Read from nonexistant table") {
    val ids = for {
      s <- session
      i <- s.read("res.partnerzoid", 1)
    } yield i

    ids.onComplete((value: Try[List[List[(String, Any)]]]) => value match {
      case Success(s) => s.foreach(_ foreach println)
      case Failure(f) => fail(f)
    })

    intercept[OpenERPException]{
      Await.result(ids, 1 seconds)
    }

  }


  test("Search and read the res.partner table") {

    val allObs = for {
      s <- session
      obs <- s.searchAndRead("res.partner")
    } yield obs

    allObs.onComplete((value: Try[List[List[(String, Any)]]]) => value match {
      case Success(s) => println("success")
      case Failure(f) => fail(f)
    }  )
    //this one take a while...
    Await.result(allObs, 10 seconds)
  }

  test("Search and read the res.partner table with a domain and fields") {

    val allObs = for {
      s <- session
      obs <- s.searchAndRead("res.partner", ("email" ilike "info@") AND (("is_company" =/= true) OR ("zip" === "60623")), "category_id")
    } yield obs

    allObs.onComplete((value: Try[List[List[(String, Any)]]]) => value match {
      case Success(s) => s.foreach(_ foreach println)
      case Failure(f) => fail(f)
    })

    Await.result(allObs, 1 seconds)
  }


  test("Create new partner in res.partner") {

    val result = for {
      s <- session
      r <- s.create("res.partner",List("name" -> "McLovin"))
    } yield r

    result.onComplete((value: Try[Int]) => value match {
      case Success(s) => println("new Partner created with ID: " + s)
      case Failure(f) => fail(f)
    })
    Await.result(result, 10 seconds)
  }

  test("Create new partner in res.partner with invalid field") {

    val result = for {
      s <- session
      r <- s.create("res.partner",List("namsdf09je" -> "McLovin"))
    } yield r

    result.onComplete((value: Try[Int]) => value match {
      case Success(s) => println("new Partner created with ID: " + s)
      case Failure(f) => fail(f)
    })

    intercept[OpenERPException]{
      Await.result(result, 10 seconds)
    }
  }

  test("Update partner in res.partner") {

    val result = for {
      s <- session
      ids <- s.search("res.partner", "name" === "McLovin")
      r <- s.write("res.partner",ids, List("name" -> "McLovinUpdated"))
    } yield { r}

    //OpenERP returns TRUE whether we updated anything or not - useful!
    result.onComplete((value: Try[Boolean]) => value match {
      case Success(s) => println("partner updated. Or Not. Who can tell?")
      case Failure(f) => fail(f)
    })

    Await.result(result, 3 seconds)
  }

  test("Delete partner in res.partner") {

    val result = for {
      s <- session
      ids <- s.search("res.partner", "name" === "McLovingUpdated")
      d <- s.unlink("res.partner", ids)
    } yield d

    //OpenERP returns TRUE whether we deleted anything or not - useful!
    result.onComplete(_ match {
      case Success(s) => println("partner was deleted. Or not. I'm not sure.")
      case Failure(f) => fail(f)
    })
    Await.result(result, 2 seconds)
  }

  test("fail login to openerp host - bad password") {
    val session = proxy.startSession(username, password + "FAIL", database)
    session.onComplete((value: Try[OpenERPSession]) => value match{
      case Failure(f) => fail(f)
      case Success(s) => println("logged in with uid: " + s.uid)
    })
    intercept[OpenERPAuthenticationException]{
      Await.result(session, 1 second)
    }

  }

  test("fail login to openerp host - bad username") {
    val session = proxy.startSession(username+"092j3f09jfsd", password, database)
    session.onComplete((value: Try[OpenERPSession]) => value match{
      case Failure(f) => fail(f)
      case Success(s) => println("logged in with uid: " + s.uid)
    })
    intercept[OpenERPAuthenticationException]{
      Await.result(session, 1 second)
    }

  }

  test("fail login to openerp host - invalid db") {
    val session = proxy.startSession(username, password, database+"FAIL")
    session.onComplete((value: Try[OpenERPSession]) => value match{
      case Failure(f) => fail(f)
      case Success(s) => println("logged in with uid: " + s.uid)
    })
    intercept[OpenERPException]{
      Await.result(session, 1 second)
    }

  }

}
