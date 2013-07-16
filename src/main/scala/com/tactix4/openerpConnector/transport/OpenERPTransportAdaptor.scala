package com.tactix4.openerpConnector.transport

import scala.annotation.implicitNotFound

import scala.concurrent.Future


/**
 * @author max@tactix4.com
 *         7/10/13
 *
 *         Adaptor to be implemented for each transport mechanism supported
 *         currently only xmlrpc but soon to be jsonrpc
 */
trait OpenERPTransportAdaptor {
  def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: List[TransportDataType]) : Future[TransportResponse]
  def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: TransportDataType*): Future[TransportResponse]= {
    sendRequest(config, methodName,params.toList)
  }
}


class PimpedAny[T](any: T) {
  def toTransportDataType(implicit writer: TransportDataWriter[T]): TransportDataType = writer.write(any)
}

sealed abstract class TransportDataType {
  def convertTo[T: TransportDataReader]: T = transportDataReader[T].read(this)
  def toScalaType:Any
}

case class TransportNumber[T](value: T)(implicit num: Numeric[T]) extends TransportDataType{
  def toScalaType:T = value

}

case class TransportBoolean(value: Boolean) extends TransportDataType{
  def toScalaType:Boolean = value
}

case class TransportString(value: String) extends TransportDataType{
  def toScalaType:String = value
}

case class TransportArray(value: List[TransportDataType]) extends TransportDataType{
  def toScalaType:List[Any] = value.map(_.toScalaType)
}

case class TransportMap(value: List[(TransportString, TransportDataType)]) extends TransportDataType {
  def toScalaType:List[(String,Any)] = value.map((t: (TransportString, TransportDataType)) => (t._1.value, t._2.toScalaType))
}

case class TransportNull(value: Null) extends TransportDataType{
 def toScalaType = null
}

@implicitNotFound(msg = "Can not find TransportDataReader for type ${T}")
trait TransportDataReader[T] {
  def read(obj: TransportDataType): T
}

object TransportDataReader {
  implicit def func2Reader[T](f: TransportDataType => T): TransportDataReader[T] = new TransportDataReader[T] {
    def read(obj: TransportDataType) = f(obj)
  }
}

@implicitNotFound(msg = "Can not find TransportDataWriter for type ${T}")
trait TransportDataWriter[T] {
  def write(obj: T): TransportDataType
}

object TransportDataWriter {
  implicit def func2Writer[T](f: T => TransportDataType): TransportDataWriter[T] = new TransportDataWriter[T] {
    def write(obj: T) = f(obj)
  }
}

trait TransportDataFormat[T] extends TransportDataWriter[T] with TransportDataReader[T] {


}


case class OpenERPTransportAdaptorConfig(protocol: String, host: String, port: Int, var path: String, headers: Map[String, String] = Map())
