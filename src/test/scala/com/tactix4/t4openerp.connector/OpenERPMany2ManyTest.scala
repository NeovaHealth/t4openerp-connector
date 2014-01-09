/*
 * Copyright (C) 2013 Tactix4
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tactix4.t4openerp.connector

import com.typesafe.config._
import org.scalatest.concurrent._
import org.scalatest.FunSuite
import scala.concurrent.{Future, Await}
import scala.concurrent.Future._
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.t4openerp.connector.domain.Domain._

import com.tactix4.t4openerp.connector.transport._
import OpenERPSession._
import com.tactix4.t4openerp.connector.exception.OpenERPException

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

  val proxy = new OpenERPConnector("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)

  test("Query category_id field in res.partner") {
    val result = for {
      s <- session
      r <- s.searchAndRead(model="res.partner", fields="category_id")
    } yield r

    result.onComplete((value: Try[ResultType]) => value match{
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

    result.onComplete((value: Try[ResultType]) => value match{
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
      update <- s.write("res.partner",ids, Map("category_id"-> List(10,13)))
      //read starting values
      startVal <- s.read("res.partner", ids, List("category_id"))
      //update values
      update <- s.write("res.partner",ids, Map("category_id"-> List(10,8)))
      //read values again to check they've been changed
      check <- s.read("res.partner", ids, List("category_id"))

    } yield {println(startVal); check != startVal}

    result.onComplete {
      case Success(s) => s match {
        case true => println("Successful write")
        case false => fail("Unsuccessful write")
      }
      case Failure(f) => fail(f)
    }

    Await.result(result, 5 seconds)
  }

  test("Fail on Update category_id with invalid value") {
    val result = for {
      s <- session
      ids <- s.search("res.partner", "category_id" =/= false, limit=1)
      failwrite <- s.write("res.partner", ids, Map("category_id" -> "Some value that doesn't make sense"))
    } yield failwrite
    intercept[OpenERPException]{
      Await.result(result, 2 seconds)
    }
  }


}
