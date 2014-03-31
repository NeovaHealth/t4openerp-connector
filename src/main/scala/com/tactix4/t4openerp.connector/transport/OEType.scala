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

package com.tactix4.t4openerp.connector.transport

import scalaz.{Semigroup, Monoid}


/**
 * @author max@tactix4.com
 *         24/08/2013
 */

/**
 * Hierarchy for the internal data representation
 *
 * This subset of datastructures/types were chosen given the overlap between
 * the supported types in json and xmlrpc
 */


sealed trait OEType{

    def fold[X](
    oerpBoolean: Boolean => X,
    oerpNumeric: BigDecimal => X,
    oerpString: String => X,
    oerpArray: List[OEType] => X,
    oerpMap: Map[String,OEType] => X,
    oerpNull: Null => X
  ): X =
    this match {
      case OEBoolean(b)   => oerpBoolean(b)
      case OENumber(n)    => oerpNumeric(n)
      case OEString(s)    => oerpString(s)
      case a:OEArray      => oerpArray(a.value)
      case m:OEDictionary => oerpMap(m.value)
      case OENull         => oerpNull(null)
    }

  override def toString: String = this match{
    case OEBoolean(b)   => b.toString
    case OENumber(n)    => n.toString
    case OEString(s)    => s.toString
    case a:OEArray      => a.value.toString
    case m:OEDictionary => m.value.toString
    case OENull         => "<NULL>"
  }

  def bool : Option[Boolean]                  = this.fold(b => Some(b),_=>None,_=>None,_=>None,_=>None,_=>None)
  def number : Option[BigDecimal]             = this.fold(_ => None,n=> Some(n),_=>None,_=>None,_=>None,_=>None)
  def double : Option[Double]                 = this.fold(_ => None,n=> Some(n.doubleValue),_=>None,_=>None,_=>None,_=>None)
  def int : Option[Int]                       = this.fold(_ => None,n=> Some(n.intValue),_=>None,_=>None,_=>None,_=>None)
  def string : Option[String]                 = this.fold(_ => None,_=> None,s=>Some(s),_=>None,_=>None,_=>None)
  def array : Option[List[OEType]]            = this.fold(_ => None,_=> None,_=>None,a => Some(a),_=> None,_=>None)
  def dictionary : Option[Map[String,OEType]] = this.fold(_ => None,_=> None,_=>None,_=>None,s=>Some(s),_=>None)
  def nullT : Option[Nothing]                 = this.fold(_ => None,_=> None,_=>None,_=>None,_=>None,_ => null)

  def isBool :Boolean       = this.fold(_ => true, _ => false,_ => false,_ => false,_ => false,_ => false)
  def isNumber :Boolean     = this.fold(_ => false,_ => true, _ => false,_ => false,_ => false,_ => false)
  def isInt :Boolean        = this.fold(_ => false,n => n.isValidInt, _ => false,_ => false,_ => false,_ => false)
  def isDouble :Boolean     = this.fold(_ => false,n => n.isValidDouble, _ => false,_ => false,_ => false,_ => false)
  def isString :Boolean     = this.fold(_ => false,_ => false,_ => true, _ => false,_ => false,_ => true)
  def isArray :Boolean      = this.fold(_ => false,_ => false,_ => false,_ => true, _ => false,_ => false)
  def isDictionary :Boolean = this.fold(_ => false,_ => false,_ => false,_ => false,_ => true, _ => false)
  def isNull :Boolean       = this.fold(_ => false,_ => false,_ => false,_ => false,_ => false,_ => true)

  def asBool[X](f: Boolean => X): Option[X] = bool.map(f)
  def asNumber[X](f: BigDecimal => X): Option[X] = number.map(f)
  def asInt[X](f: Int => X) = int.map(f)
  def asDouble[X](f: Double => X) = double.map(f)
  def asString[X](f: String => X) = string.map(f)
  def asArray[X](f: List[OEType] => X) = array.map(f)
  def asDictionary[X](f: Map[String,OEType] => X) = dictionary.map(f)

}

case class OENumber(value: BigDecimal) extends OEType
case class OEBoolean(value: Boolean) extends OEType
case class OEString(value: String) extends OEType
class OEArray(val value: List[OEType]) extends OEType
class OEDictionary(val value: Map[String, OEType]) extends OEType{
  def get(k:String) : Option[OEType] = value.get(k)
}
case object OENull extends OEType

object OEArray{
  def apply(l:List[OEType]) : OEArray = new OEArray(l)
  def apply(l:OEType*) : OEArray = new OEArray(l.toList)
  def unapplySeq(a: OEArray) : Option[List[OEType]] = Some(a.value)

  val monoidInstance = new Monoid[OEArray]{
    override def zero: OEArray = OEArray(Nil)

    override def append(f1: OEArray, f2: => OEArray): OEArray = OEArray(f1.value ++ f2.value)
  }
  implicit val semiGroupInstance = new Semigroup[OEArray] {
    override def append(f1: OEArray, f2: => OEArray):OEArray = OEArray(f1.value ++ f2.value)
  }

}
object OEDictionary{
  def apply(l:Map[String,OEType]) : OEDictionary = new OEDictionary(l)
  def apply(l:(String,OEType)*) : OEDictionary = new OEDictionary(l.toMap)
  def unapply(a: OEDictionary) : Option[Map[String,OEType]] = Some(a.value)

  val monoidInstance = new Monoid[OEDictionary]{
    override def zero: OEDictionary = OEDictionary()
    override def append(f1: OEDictionary, f2: => OEDictionary): OEDictionary = OEDictionary(f1.value ++ f2.value)
  }
   implicit val semiGroupDictionary = new Semigroup[OEDictionary] {
    override def append(f1: OEDictionary, f2: => OEDictionary): OEDictionary = OEDictionary(f1.value ++ f2.value)
   }
}
