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

package com.tactix4.openerpConnector

import org.scalatest.FunSuite
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Awaitable, Await, Future}
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

/**
 * @author max@tactix4.com
 *         16/08/2013
 */
class OpenERPSessionTest extends FunSuite {



  val conf = ConfigFactory.load()

  val username    = conf.getString("openERPServer.username")
  val password    = conf.getString("openERPServer.password")
  val database    = conf.getString("openERPServer.database")
  val openerpHost = conf.getString("openERPServer.hostname")
  val openerpPort = conf.getInt("openERPServer.port")

  val proxy = new OpenERPConnector("http", openerpHost,openerpPort)

  val session = proxy.startSession(username,password,database)


  test("making sure the modelAdaptor memoized hashmap is memoizing"){

    /**
     * ghetto timing function
     *
     * @param a the function to test
     * @tparam A the function type (must be subtype of Awaitable[_])
     * @return the time taken to execute the function
     */

    def time[A <: Awaitable[_]](a: => Awaitable[_]) = {
      val now = System.nanoTime
      Await.result(a, 10 seconds)
      (System.nanoTime - now) / 1000
    }

    val r = time { session.flatMap(_.getFields("res.users"))}
    val r2 = time{ session.flatMap(_.getFields("res.users"))}
    val r3 = time{ session.flatMap(_.getFields("res.users"))}
    val r4 = time{ session.flatMap(_.getFields("res.users"))}
    val r5 = time{ session.flatMap(_.getFields("res.users"))}

    assert(r2 < r && r3 < r && r4 < r && r5 < r)

    val t = time{ session.flatMap(_.getFields("res.partner"))}
    val t2 = time{ session.flatMap(_.getFields("res.partner"))}
    val t3 = time{ session.flatMap(_.getFields("res.partner"))}
    val t4 = time{ session.flatMap(_.getFields("res.partner"))}
    val t5 = time{ session.flatMap(_.getFields("res.partner"))}

    assert(t2 < t && t3 < t && t4 < t && t5 < t)




  }

}
