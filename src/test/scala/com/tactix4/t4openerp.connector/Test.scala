package com.tactix4.t4openerp.connector

import org.scalatest.FunSuite

import com.typesafe.config._
import org.scalatest.concurrent._
import org.scalatest.FunSuite
import scala.concurrent.{Future, Await}
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.t4openerp.connector.domain.Domain._
import OpenERPSession._
import com.tactix4.t4openerp.connector.exception.{OpenERPAuthenticationException, OpenERPException}
import com.tactix4.t4openerp.connector.transport.{TransportNull, TransportString, TransportNumber, TransportArray}

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 10/01/14
 * Time: 15:55
 * To change this template use File | Settings | File Templates.
 */
class Test extends FunSuite{

 val conf = ConfigFactory.load()

  val username    = conf.getString("openERPServer.username")
  val password    = conf.getString("openERPServer.password")
  val database    = conf.getString("openERPServer.database")
  val openerpHost = conf.getString("openERPServer.hostname")
  val openerpPort = conf.getInt("openERPServer.port")

  val proxy = new OpenERPConnector("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)

  test("some other test stuff") {

    val result = for {
      s <- session
      r <- s.read[String]("res.partner", List(1,2,3,4,5,6),List("name","id"))
    } yield r

    result.onComplete{
      case Success(s) => s.map((m: Map[String, String]) => println(s))
      case Failure(f) => fail(f)
    }
    Await.result(result, 2 seconds)
  }


//  test("test some stuff") {
//      val r = for {
//        s <- session
//        taskTypeId  <- s.search("t4clinical.task.type", "name" === "AssignBed")
//        vOfTasks    <- s.searchAndRead[Int]("t4clinical.task.base", "task_type_id" === taskTypeId.head AND "state" =/= "done", List("visit_id", "task_id", "id"))
//        posOfTasks  <- s.read[Int]("t4clinical.patient.visit",vOfTasks.map(_("visit_id").asInstanceOf[List[_]].head.toString.toInt), List("pos_location"))
//        employeePos <- s.searchAndRead[Int]("hr.employee", "user_id" === s.uid, List("pos_location_ids"))
//        employeePosIds <- Future.successful(employeePos.flatMap(_("pos_location_ids").asInstanceOf[List[_]].map(_.toString.toInt)))
//        posId       <- Future.successful(posOfTasks.map(_("pos_location").asInstanceOf[List[_]].head.toString.toInt))
//        allTaskId   <- Future.successful(vOfTasks.map(_("id")))
//        taskIds     <- Future.successful((allTaskId zip posId).filter(x => employeePosIds contains x._2 ).map(_._1))
//        n           <- s.searchAndRead[String]("t4clinical.pos.delivery", "id"  in TransportArray(employeePosIds.map(TransportNumber(_))), "name")
//        names       <- Future.successful{n.map(_("name").value)}
//        freeBeds    <- s.search("t4clinical.pos.delivery", "parent_id" in TransportArray(names.map(TransportString)) AND "occupants" === TransportNull)
//        result      <- s.callMethod[Boolean]("t4clinical.task.base","assignLocationFromTask",TransportNumber(taskIds.head), TransportNumber(freeBeds.head))
//      } yield result
//
//
//    r.onComplete {
//      case Success(s) => println("result: " + s)
//        case Failure(f) => fail(f)
//      }
//      Await.result(taskIds, 2 minutes)
//  }
}
