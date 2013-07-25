package com.tactix4.openerpConnector

/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object transport {

  def transportDataReader[T](implicit reader: TransportDataReader[T]) = reader
  def transportDataWriter[T](implicit writer: TransportDataWriter[T]) = writer
  implicit def pimpAny[T](any: T) = new PimpedAny(any)
  implicit def StringToTransportString(s: String) = new TransportString(s)
  implicit def BoolToTransportString(b:Boolean) = new TransportBoolean(b)
  implicit def IntToTransportNumber(i: Int) = new TransportNumber(i)
  implicit def FloatToTransportNumber(i: Float) = new TransportNumber(i)
  implicit def DoubleToTransportNumber(i: Double) = new TransportNumber(i)
  implicit def ListOfIntsToTransportArray(l: List[Int]) : TransportArray = TransportArray(l.map(TransportNumber(_)))
  implicit def ListOfStringsToTransportArray(l: List[String]) : TransportArray = TransportArray(l.map(TransportString(_)))
  implicit def ListMaptoTransportMap(l: List[(String, String)]) : TransportMap = TransportMap(l.map(x => (TransportString(x._1), TransportString(x._2))))
  type TransportResponse = Either[String,TransportDataType]
}
