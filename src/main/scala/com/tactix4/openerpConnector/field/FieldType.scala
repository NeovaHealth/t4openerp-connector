package com.tactix4.openerpConnector.field

import java.util.Date

/**
 * @author max@tactix4.com
 *         17/07/2013
 */
sealed trait Field
sealed abstract class FieldType[T](val value: T) extends Field
//TODO:Include the other Field Types many-to-many e.t.c.
object FieldType {
  import com.tactix4.openerpConnector.transport._
  implicit def BoolToFieldType(value: Boolean): FieldType[Boolean] = new BooleanField(value)

  implicit def IntToFieldType(value: Int): FieldType[Int] = new IntField(value)

  implicit def FloatToFieldType(value: Float): FieldType[Float] = new FloatField(value)

  implicit def StringToFieldType(value: String): FieldType[String] = new TextField(value)

  implicit def DateToFieldType(value: Date): FieldType[Date] = new DateField(value)

  implicit def FieldToTransportType(f: Field): TransportDataType = f match {
    case x: BooleanField => TransportBoolean(x.value)
    case x: IntField => TransportNumber(x.value)
    case x: FloatField => TransportNumber(x.value)
    case x: TextField => TransportString(x.value)
    case x: DateField => TransportString(x.value.toString)


  }


}

//Simple Types
case class BooleanField(b: Boolean) extends FieldType[Boolean](b)
case class DateField(d: Date) extends FieldType[Date](d)
case class DateTimeField(d: Date) extends FieldType[Date](d)
case class FloatField(f: Float) extends FieldType[Float](f)
case class TextField(s: String) extends FieldType[String](s)
case class CharField(s: String,length: Int) extends FieldType[String](s)
case class IntField(i: Int) extends FieldType[Int](i)
case class SelectionField(s: String) extends FieldType[String](s)
case class BinaryField(b: Array[Byte]) extends FieldType[Array[Byte]](b)

//RelationalTypes
case class Many2One(otherObjectName: String, fieldName: String) extends FieldType[String](otherObjectName)
case class One2Many(otherObjectName: String, fieldRelationId: String, fieldName: String) extends FieldType[String](otherObjectName)
case class Many2Many(otherObjectName: String, relationObject: String, actualObjectId: String, otherObjectId: String, fieldName:String) extends FieldType[String](otherObjectName)


