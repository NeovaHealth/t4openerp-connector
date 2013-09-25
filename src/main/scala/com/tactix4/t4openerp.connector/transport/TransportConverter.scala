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
@implicitNotFound(msg = "Can not find TransportDataConverter for type ${T}")
trait TransportDataConverter[T] {
  def read(obj: TransportDataType): T
  def write(obj: T): TransportDataType
}

/**
 *
 * The PimpedAny simply provides a toTransportDataType convenience method
 *
 * @param any
 * @tparam T
 */
class PimpedAny[T](any: T) {
  def toTransportDataType(implicit writer: TransportDataConverter[T]): TransportDataType = implicitly(writer).write(any)
}