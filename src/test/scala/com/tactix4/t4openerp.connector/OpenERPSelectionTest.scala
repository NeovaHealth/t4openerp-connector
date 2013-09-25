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
import scala.concurrent.Await
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.tactix4.t4openerp.connector.OpenERPSession._
import com.tactix4.t4openerp.connector.transport._
import com.tactix4.t4openerp.connector.exception.OpenERPException

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

  val proxy = new OpenERPConnector("http", openerpHost,openerpPort)

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
