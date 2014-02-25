package com.tactix4.t4openerp.connector.transport

import org.scalatest.{ShouldMatchers, FunSuite}

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 18/12/13
 * Time: 12:04
 * To change this template use File | Settings | File Templates.
 */
class TransportMapTest extends FunSuite with ShouldMatchers {

  test("Test filter on TransportMap"){
    val t = TransportMap(Map("one" -> TransportNumber(2), "two" -> TransportNumber(2), "three" -> TransportNumber(3)))
    val s =  t.filter(_._2 match {
      case x:TransportNumber[Int] => x.value > 2
      case _ => false
    })

    s.value should have size 1
    s.value should contain ("three" -> TransportNumber(3))
  }

  test("Test mapValues on TransportMap"){

    val t = TransportMap(Map("one" -> TransportNumber(2), "two" -> TransportNumber(2), "three" -> TransportNumber(3)))
    val s = t.mapValues((dataType: OERPType) => TransportString(dataType.toString))

    assert (s.value.forall((v: (String, OERPType)) => v._2 match {
      case x:TransportString => true
      case _ => false
    }))
  }
}
