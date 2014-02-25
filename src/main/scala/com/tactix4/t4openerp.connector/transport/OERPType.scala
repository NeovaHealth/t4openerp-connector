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

sealed trait OERPType{
    def fold[X](
    oerpBoolean: Boolean => X,
    oerpNumeric: Double => X,
    oerpString: String => X,
    oerpArray: List[OERPType] => X,
    oerpMap: Map[String,OERPType] => X,
    oerpNull: Null => X
  ): X =
    this match {
      case OERPBoolean(b)   => oerpBoolean(b)
      case OERPNumber(n)       => oerpNumeric(n)
      case OERPString(s)    => oerpString(s)
      case OERPArray(a)     => oerpArray(a)
      case OERPMap(o)    => oerpMap(o)
      case OERPNull         => oerpNull(null)
    }

  def bool : Option[Boolean]                = this.fold(b => Some(b),_=>None,_=>None,_=>None,_=>None,_=>None)
  def number : Option[Double]              = this.fold(_ => None,n=> Some(n),_=>None,_=>None,_=>None,_=>None)
  def string : Option[String]               = this.fold(_ => None,_=> None,s=>Some(s),_=>None,_=>None,_=>None)
  def array : Option[List[OERPType]]        = this.fold(_ => None,_=> None,_=>None,a => Some(a),_=> None,_=>None)
  def struct : Option[Map[String,OERPType]] = this.fold(_ => None,_=> None,_=>None,_=>None,s=>Some(s),_=>None)
  def nullT : Option[Nothing]               = this.fold(_ => None,_=> None,_=>None,_=>None,_=>None,_ => null)

  def isBool :Boolean   = this.fold(_ => true, _ => false,_ => false,_ => false,_ => false,_ => false)
  def isNumber :Boolean = this.fold(_ => false,_ => true, _ => false,_ => false,_ => false,_ => false)
  def isString :Boolean = this.fold(_ => false,_ => false,_ => true,_ => false,_ => false,_ => true)
  def isArray :Boolean  = this.fold(_ => false,_ => false,_ => false,_ => true,_ => false,_ => false)
  def isStruct :Boolean = this.fold(_ => false,_ => false,_ => false,_ => false,_ => true,_ => false)
  def isNull :Boolean   = this.fold(_ => false,_ => false,_ => false,_ => false,_ => false,_ => true)
}
case class OERPNumber(value: Double) extends OERPType
case class OERPBoolean(value: Boolean) extends OERPType
case class OERPString(value: String) extends OERPType
case class OERPArray(value: List[OERPType]) extends OERPType
case class OERPMap(value: Map[String, OERPType]) extends OERPType
case object OERPNull extends OERPType







