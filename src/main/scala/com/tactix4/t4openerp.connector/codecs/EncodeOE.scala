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

import com.tactix4.t4openerp.connector.CodecResult
import com.tactix4.t4openerp.connector.transport.OEType

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

/**
 *
 * Encoding/Decoding Type Classes
 * @author max@tactix4.com
 *         24/08/2013
 */

@implicitNotFound(msg = "Can not find OEDataDecoder for type ${T}")
trait OEDataDecoder[T]{
  def decode(obj: OEType): CodecResult[T]
}
@implicitNotFound(msg = "Can not find OEDataEncoder for type ${T}")
trait OEDataEncoder[T]{
  def encode(obj: T): CodecResult[OEType]
}
object OEDataDecoder {
  def apply[T](r: OEType => CodecResult[T]) : OEDataDecoder[T] = new OEDataDecoder[T] {
    def decode(obj: OEType): CodecResult[T] = r(obj)
  }
}
object OEDataEncoder{
  def apply[T](r: T => CodecResult[OEType]) : OEDataEncoder[T] = new OEDataEncoder[T] {
    def encode(obj: T): CodecResult[OEType] = r(obj)
  }
}
