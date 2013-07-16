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
  implicit def IntToTransportNumber(i: Int) = new TransportNumber(i)
  implicit def ListOfIntsToTransportArray(l: List[Int]) : TransportArray = TransportArray(l.map(TransportNumber(_)))
  type TransportResponse = Either[TransportString,List[TransportDataType]]
}
