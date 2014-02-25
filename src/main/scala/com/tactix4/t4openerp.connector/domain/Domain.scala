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

package com.tactix4.t4openerp.connector.domain

import com.tactix4.t4openerp.connector.transport._
import com.tactix4.t4openerp.connector._
import scala.language.implicitConversions

/**
 * A trait to specify Domains for [[com.tactix4.t4openerp.connector.OpenERPSession]] queries
 *
 * Used in conjunction with the implicits defined in Domains companion object, a DSL for specifying complex
 * domains is provided
 *
 * {{{
 *
 * val complexDomain = ("name" === "ABC") AND ("lang" =/= "EN") AND (NOT("department" child_of "HR") OR ("country" like "Germany"))
 *
 * }}}
 *
 * Associativity is to the '''LEFT''' - so parenthesis should be used to make precedence unambiguous
 *
 * {{{
 *
 * // equates to all all Jim's with ID's of one, OR Jills.
 * val ambig = ("id" === 1) AND ("name" === "jim") OR ("name" == "jill")
 *
 * // equates to any Jim or Jill with an id of 1
 * val nonAmbig = ("id" === 1) AND (("name" === "jim") OR ("name" == "jill"))
 *
 * }}}
 *
 * Domain's subclasses [[com.tactix4.t4openerp.connector.domain.AND]] and [[com.tactix4.t4openerp.connector.domain.OR]] build up tree structures
 * of [[com.tactix4.t4openerp.connector.domain.DomainTuple]] expressions, which are constructed from a fieldName, an operator and a value.
 *
 * [[com.tactix4.t4openerp.connector.domain.NOT]] can be applied only to a single DomainTuple and for that reason its factory method is found in
 * DomainTuple rather than Domain
 *
 *
 *
 * @author max@tactix4.com
 *  7/9/13
 */

sealed abstract class Domain {
  /**
   * A factory method for the [[com.tactix4.t4openerp.connector.domain.AND]] class
   * @param that the domain to AND with
   * @return a Domain which represents the conjunction of the argument and the calling object
   */
  def AND(that: Domain) : Domain = new AND(this, that)

  /**
   * A factory method for the [[com.tactix4.t4openerp.connector.domain.OR]] class
   * @param that the domain to OR with
   * @return a Domain which represents the disjunction of the argument and the calling object
   */
  def OR(that: Domain) : Domain = new OR(this, that)
}

/**
 * A class to represent a conjunction of two Domains
 * @param left
 * @param right
 */
case class AND(left: Domain, right: Domain) extends Domain {
  override def toString = left + "," + right
}

/**
 * A class to represent a disjunction of two Domains
 * @param left
 * @param right
 */
case class OR(left: Domain, right: Domain) extends Domain {
  override def toString = "'|'," + left + "," + right
}

/**
 * A class to represent a negation of a DomainTuple
 * @param value
 */
case class NOT(value: DomainTuple) extends Domain {
  override def toString = "'!'," + value
}

/**
 * A class to represent a leaf-node in our tree structure of logical operations
 *
 * It constitutes one unit of domain filtering - to be combined with [[com.tactix4.t4openerp.connector.domain.AND]], [[com.tactix4.t4openerp.connector.domain.OR]],
 * or negated with [[com.tactix4.t4openerp.connector.domain.NOT]]
 * @param fieldName the name of the field on which we are applying a filter
 * @param operator the operator of the filter
 * @param value the value of the filter
 */
case class DomainTuple(fieldName: String, operator: String, value:OERPType) extends Domain {
  override def toString = "('" + fieldName + "','" + operator + "','" + value + "')"

  /**
   * Factory method for [[com.tactix4.t4openerp.connector.domain.NOT]] class
   * @return a negated version of this
   */
  def NOT = new NOT(this)
}

/**
 * A class used to construct DomainTuples - used via the implicit in the Domain's companion object
 * @param s the fieldName on which to apply the domain filter
 */
final class DomainOperator(s: String) {

  /**
   * Equality
   * @param n the value of equality we are testing for
   * @return a DomainTuple representing equality
   */
  def ===(n: OERPType)           = DomainTuple(s, "=", n)

  /**
   * Inequality
   * @param n the value of inequality we are testing for
   * @return a DomainTuple representing inequality
   */
  def =/=(n: OERPType)           = DomainTuple(s, "!=", n)

  /**
   * LessThan
   * @param n the value which we are asserting is LessThan `this`
   * @return a DomainTuple representing a LessThan domain filter
   */
  def lt(n: OERPType)            = DomainTuple(s, "<", n)
  /**
   * GreaterThan
   * @param n the value which we are asserting is GreaterThan `this`
   * @return a DomainTuple representing a GreaterThan domain filter
   */
  def gt(n: OERPType)            = DomainTuple(s, ">", n)

  /**
   * Like
   * @param n the value which `this` is like
   * @return a DomainTuple representing an SQLish like pattern match
   */
  def like(n: OERPType)          = DomainTuple(s, "like", n)

  /**
   * Case insensitive Like
   * @param n the value which `this` is like
   * @return a DomainTuple representing an SQLish like pattern match - case insensitive
   */
  def ilike(n: OERPType)         = DomainTuple(s, "ilike", n)

  /**
   * In
   * @param n a list or tuple that the value of `this` should be in
   * @return a DomainTuple representing a test for s being in n
   */
  def in(n: OERPType)            = DomainTuple(s, "in", n)
  /**
   * Not In
   * @param n a list or tuple that the value of `this` should not be in
   * @return a DomainTuple representing a test for s not being in n
   */
  def not_in(n: OERPType)        = DomainTuple(s, "not in", n)

  /**
   * Child Of
   * The child_of operator will look for records who are children or grand-children of a given record,
   * according to the semantics of this model (i.e following the relationship field named by self._parent_name, by default parent_id.
   * @param n the parent of `this`
   * @return a DomainTuple representing a test for a child_of relationship
   */
  def child_of(n: OERPType)      = DomainTuple(s, "child_of", n)

  /**
   * parent left
   * @see http://stackoverflow.com/questions/11861436/parent-left-and-parent-right-in-openerp
   * @param n the subject being tested for being a child of `this`
   * @return a DomainTuple representing a test for a parent_left relationship
   */
  def parent_left(n: OERPType)   = DomainTuple(s, "parent_left", n)
  /**
   * parent right
   * @see [[http://stackoverflow.com/questions/11861436/parent-left-and-parent-right-in-openerp]]
   * @param n the subject being tested for being a child of `this`
   * @return a DomainTuple representing a test for a parent_right relationship
   */
  def parent_right(n: OERPType)  = DomainTuple(s, "parent_right", n)
}

/**
 * Companion object for [[com.tactix4.t4openerp.connector.domain.Domain]] providing some implicit conversions
 * as well as an implicit object for conversion to TransportDataTypes
 */
object Domain {

  /**
   * Implicit to convert a Domain to an Option[Domain], used to clarify request parameters in [[com.tactix4.t4openerp.connector.OpenERPSession]]
   * @param d the Domain
   * @return an Option[d]
   */
  implicit def DomainToOptionDomain(d: Domain) : Option[Domain] = Some(d)

  /**
   * Implicit to convert a string to a DomainOperator.
   * This is the main starting point for creating Domains
   * @param s the string which goes on to represent the fieldName of a DomainTuple
   * @return a DomainOperator
   */
  implicit def StringToDomainOperator(s: String): DomainOperator = new DomainOperator(s)


  /**
   * Provides the TransportDataConverter[Domain] implementation which enables the [[com.tactix4.t4openerp.connector.transport.PimpedAny]]
   * call to .toTransportDataType()
   */
  implicit object DomainToTransportData extends TransportDataConverter[Domain]{
    //TODO: make tail recursive
    def write(obj: Domain): OERPType = {
      def loop(tree: Domain): List[OERPType] =
        tree match {
          case e: AND => TransportString("&") :: loop(e.left) ++ loop(e.right)
          case e: OR => TransportString("|") :: loop(e.left) ++ loop(e.right)
          case e: NOT => TransportString("!") :: TransportArray(List(TransportString(e.value.fieldName), TransportString(e.value.operator), e.value.value)) :: Nil
          case e: DomainTuple => TransportArray(List(TransportString(e.fieldName), TransportString(e.operator), e.value)) :: Nil
        }
      TransportArray(loop(obj))
    }
     def read(obj: OERPType) = ??? //not needed
   }
}




















