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

package com.tactix4.t4openerp.connector.codecs

import com.tactix4.t4openerp.connector.transport.OEType

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

/**
 *
 * Typeclass for converting to-from the transport data type
 * Any Transport implementation should instantiate an object of type [[com.tactix4.t4openerp.connector.transport.TransportDataConverter[TheirType]]]
 * and provide the read/write methods

 * @author max@tactix4.com
 *         24/08/2013
 */
@implicitNotFound(msg = "Can not find OEDataConverter for type ${T}")
trait OEDataConverter[T] extends OEDataDecoder[T] with OEDataEncoder[T]

@implicitNotFound(msg = "Can not find OEDataDecoder for type ${T}")
trait OEDataDecoder[T]{
  def decode(obj: OEType): DecodeResult[T]
}
@implicitNotFound(msg = "Can not find OEDataEncoder for type ${T}")
trait OEDataEncoder[T]{
  def encode(obj: T): OEType
}
object OEDataDecoder {
  def apply[T](r: OEType => DecodeResult[T]) : OEDataDecoder[T] = new OEDataDecoder[T] {
    def decode(obj: OEType): DecodeResult[T] = r(obj)
  }
}
object OEDataEncoder{
  def apply[T](r: T => OEType) : OEDataEncoder[T] = new OEDataEncoder[T] {
    def encode(obj: T): OEType = r(obj)
  }
}

object OEDataConverter{
  def apply[T](r1: OEType => DecodeResult[T])(r2: T => OEType) : OEDataConverter[T] = new OEDataConverter[T] {
    override def decode(obj: OEType): DecodeResult[T] = r1(obj)
    override def encode(obj: T): OEType = r2(obj)
  }
}

class EncodeOps[T:OEDataEncoder](any: T) {
  def encode: OEType = implicitly[OEDataEncoder[T]].encode(any)
}
class DecodeOps(any: OEType) {
   def decodeAs[T:OEDataDecoder] : DecodeResult[T] = implicitly[OEDataDecoder[T]].decode(any)

}