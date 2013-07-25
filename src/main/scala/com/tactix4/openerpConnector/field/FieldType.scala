package com.tactix4.openerpConnector.field
import scala.reflect.runtime.universe._
import java.util.Date
import com.tactix4.openerpConnector.transport.{TransportArray, TransportDataType, TransportString, TransportMap}
import com.tactix4.openerpConnector.OpenERPException

/**
 * @author max@tactix4.com
 *         17/07/2013
 */


/**
 * Implementation of HMap derived from :
 * http://stackoverflow.com/questions/13165493/how-save-a-typetag-and-then-use-it-later-to-reattach-the-type-to-an-any-scala-2
 *
 */

trait FieldType{
  val name: String
  val parameters: TransportMap
  val fieldType = parameters.value.find(_._1.value == "type").map(_._2.toScalaType).getOrElse(throw new OpenERPException("Could not find type information"))
}

case class Field(name: String, parameters: TransportMap) extends FieldType{
}
case class SelectionField(name: String, parameters: TransportMap) extends FieldType{
  val selectionValues = parameters.value.find(_._1.value == "selection").map(_._2 match {
    case TransportArray(x)   =>  x.headOption.map( _ match{
      case TransportArray(y)      => y.map(_.toScalaType.toString)
      case _                      =>  throw new OpenERPException("Could not parse selection field: " + x)
    }).getOrElse(throw new OpenERPException("Could not parse selection field: " + x))
    case fail                => throw new OpenERPException("Could not parse selection field: " + fail)
  })
}

//TODO:Include the other FieldType Types many-to-many e.t.c.
object FieldType {

  implicit def TransportMapToField(name:String, params:TransportMap) : Option[FieldType]= {
    params.value.find(_._1 == "type").map(_._2.toScalaType match {
      case "integer" => Field(name, params)
      case "boolean" => Field(name, params)
      case "char" => Field(name, params)
      case "float" => Field(name, params)
      case "text" => Field(name, params)
      case "date" => Field(name, params)
      case "datetime" => Field(name, params)
      case "binary" => Field(name, params)
      case "selection" => SelectionField(name, params)
      case "many2one" => Field(name, params)
      case "one2many" => Field(name, params)
      case "many2many" => Field(name, params)
      case _ => throw new OpenERPException("Could not find type of field: " + params.value)
    })

  }

  implicit def ListOfFieldsToTransportArray(fields: List[Field]) : TransportArray = {
    TransportArray(fields.map(f => FieldToTransportMap(f)))
  }
  implicit def FieldToTransportMap(field: FieldType) : TransportMap = {
    TransportMap(List((TransportString(field.name),field.parameters)))

  }



//  implicit object FieldToTransportFormat extends TransportDataFormat[Field]{
//
//    //fields break down to a TransportMap i reckon
//    def write(obj: Field): TransportDataType = {
//      val keys = obj.parameters.keys
//      for(k <- keys){
//        val v = obj.parameters.get(k)
//
//      }
//      val p = keys.map((k: String) => (k, obj.parameters.get(k)))
//    }
//
//
//
//    def read(obj: TransportDataType): Field = ???
//  }



//
//  implicit def BoolToFieldType(v: Boolean): FieldType[Boolean] = new FieldType[Boolean]{
//    val value = v
//    parameters.put("type","boolean")
//  }
//
//  implicit def IntToFieldType(v: Int): FieldType[Int] = new FieldType[Int] {
//    val value = v
//    parameters.put("type", "int")
//  }
//
//  implicit def FloatToFieldType(v: Float): FieldType[Float] = new FieldType[Float]{
//    val value = v
//    parameters.put("type", "float")
//  }
//  implicit def StringToFieldType(v: String): FieldType[String] = new FieldType[String]{
//    val value = v
//    parameters.put("type", "string")
//  }
//  implicit def CharToFieldType(v: String): FieldType[String] = new FieldType[String]{
//    val value = v
//  }
//  implicit def DateToFieldType(v: Date): FieldType[Date] = new FieldType[Date]{
//    val value = v
//  }
////
////  implicit def FieldToTransportType[T](f: FieldType[T]): TransportDataType = f match {
////    case x: FieldType[T] => TransportBoolean(x.value)
////    case x: IntField => TransportNumber(x.value)
////    case x: FloatField => TransportNumber(x.value)
////    case x: TextField => TransportString(x.value)
////    case x: DateField => TransportString(x.value.toString)
////
////
////  }


}

////Simple Types
//case class BooleanField(b: Boolean) extends FieldType[Boolean](b){
//
//
//}
//case class DateField(d: Date) extends FieldType[Date](d)
//case class DateTimeField(d: Date) extends FieldType[Date](d)
//case class FloatField(f: Float) extends FieldType[Float](f)
//case class TextField(s: String) extends FieldType[String](s)
//case class CharField(s: String,length: Int) extends FieldType[String](s)
//case class IntField(i: Int) extends FieldType[Int](i)
//case class SelectionField(s: String) extends FieldType[String](s)
//case class BinaryField(b: Array[Byte]) extends FieldType[Array[Byte]](b)
//
////RelationalTypes
//case class Many2One(otherObjectName: String, fieldName: String) extends FieldType[String](otherObjectName)
//case class One2Many(otherObjectName: String, fieldRelationId: String, fieldName: String) extends FieldType[String](otherObjectName)
//case class Many2Many(otherObjectName: String, relationObject: String, actualObjectId: String, otherObjectId: String, fieldName:String) extends FieldType[String](otherObjectName)
//

