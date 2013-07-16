package com.tactix4.openerpConnector.domain


import java.util.Date
import com.tactix4.openerpConnector.transport._
import com.tactix4.openerpConnector.transport.TransportBoolean
import com.tactix4.openerpConnector.transport.TransportNumber
import com.tactix4.openerpConnector.transport.TransportArray

/**
 * @author max@tactix4.com
 *         7/9/13
 *
 *         can be used to easily specify Domains for queries e.g.
 *
 *         ("name" === "ABC") AND ("lang" =/= "EN") AND (NOT("department" child_of "HR") OR ("country" like "Germany"))
 *
 *         associativity is to the LEFT - so parenthesis should be used to make precedence unambiguous
 *
 *         ("id" === 1) AND ("name" === "jim") OR ("name" == "jill")
 *
 *         equates to all all Jim's with ID's of one, OR Jills.
 *
 *         ("id" === 1) AND (("name" === "jim") OR ("name" == "jill"))
 *
 *         equates to any Jim or Jill with an id of 1
 *
 */

sealed trait Domain {

  def AND(that: Domain) = new AND(this, that)

  def OR(that: Domain) = new OR(this, that)

  def toString: String
}

object Domain {

  import FieldType._

  type OpenERPDomain = Domain

  implicit def DomainToOptionDomain(d: Domain) : Option[Domain] = Some(d)
  implicit def StringToDomainOperator(s: String): DomainOperator = new DomainOperator(s)

  implicit def DomainTupleToTransportArray(d: DomainTuple): TransportArray = TransportArray(List(d.value._1, d.value._2, d.value._3))

  implicit def DomainToArray(t: Domain): Array[Any] = {
    //TODO: make tail recursive (trampolines?)
    def loop(tree: Domain): List[Any] = {
      tree match {
        case e: AND => loop(e.left) ++ loop(e.right)
        case e: OR => "|" :: loop(e.left) ++ loop(e.right)
        case e: NOT => "!" :: Array(e.value.value._1, e.value.value._2, e.value.value._3) :: Nil
        case e: DomainTuple => Array(e.value._1, e.value._2, e.value._3) :: Nil
      }
    }
    loop(t).toArray
  }

  implicit object TreeToTransportData extends TransportDataFormat[Domain]{

    def write(obj: Domain): TransportDataType = {
      def loop(tree: Domain): List[TransportDataType] = {
        tree match {
          case e: AND => "&" :: loop(e.left) ++ loop(e.right)
          case e: OR => "|" :: loop(e.left) ++ loop(e.right)
          case e: NOT => "!" :: TransportArray(List(e.value.value._1, e.value.value._2, e.value.value._3)) :: Nil
          case e: DomainTuple => TransportArray(List(e.value._1, e.value._2, e.value._3)) :: Nil
        }
      }
      TransportArray(loop(obj))
    }
     def read(obj: TransportDataType) = ??? //not needed
   }
}

case class AND(left: Domain, right: Domain) extends Domain {
  override def toString = left + "," + right
}

case class OR(left: Domain, right: Domain) extends Domain {
  override def toString = "'|'," + left + "," + right
}

case class NOT(value: DomainTuple) extends Domain {
  override def toString = "'!'," + value
}

case class DomainTuple(value: (String, String, Field)) extends Domain {
  override def toString = "('" + value._1 + "','" + value._2 + "','" + value._3 + "')"

  def NOT = new NOT(this)
}

sealed class DomainOperator(s: String) {
  def ===(n: Field) = DomainTuple(s, "=", n)

  def =/=(n: Field) = DomainTuple(s, "!=", n)

  def lt(n: Field) = DomainTuple(s, "<", n)

  def gt(n: Field) = DomainTuple(s, ">", n)

  def like(n: Field) = DomainTuple(s, "like", n)

  def ilike(n: Field) = DomainTuple(s, "ilike", n)

  def in(n: Field) = DomainTuple(s, "in", n)

  def not_in(n: Field) = DomainTuple(s, "not in", n)

  def child_of(n: Field) = DomainTuple(s, "child_of", n)

  def parent_left(n: Field) = DomainTuple(s, "parent_left", n)

  def parent_right(n: Field) = DomainTuple(s, "parent_right", n)
}

sealed trait Field

sealed abstract class FieldType[T](val value: T) extends Field

case class BooleanFieldType(b: Boolean) extends FieldType[Boolean](b)

case class IntFieldType(i: Int) extends FieldType[Int](i)

case class FloatFieldType(f: Float) extends FieldType[Float](f)

case class StringFieldType(s: String) extends FieldType[String](s)

case class DateFieldType(d: Date) extends FieldType[Date](d)

//TODO:Include the other Field Types many-to-many e.t.c.
object FieldType {
  implicit def BoolToFieldType(value: Boolean): FieldType[Boolean] = new BooleanFieldType(value)

  implicit def IntToFieldType(value: Int): FieldType[Int] = new IntFieldType(value)

  implicit def FloatToFieldType(value: Float): FieldType[Float] = new FloatFieldType(value)

  implicit def StringToFieldType(value: String): FieldType[String] = new StringFieldType(value)

  implicit def DateToFieldType(value: Date): FieldType[Date] = new DateFieldType(value)

  implicit def FieldToTransportType(f: Field): TransportDataType = f match {
    case x: BooleanFieldType => TransportBoolean(x.value)
    case x: IntFieldType => TransportNumber(x.value)
    case x: FloatFieldType => TransportNumber(x.value)
    case x: StringFieldType => TransportString(x.value)
    case x: DateFieldType => TransportString(x.value.toString)


  }


}

