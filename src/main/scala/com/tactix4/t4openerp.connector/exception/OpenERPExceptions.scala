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

package com.tactix4.t4openerp.connector.exception

/**
 * A generic exception
 * @author max@tactix4.com
 * @param msg The exception message
 * @param f the original exception - defaults to null
 *
 */
class OpenERPException(msg: String, f: Throwable=null) extends RuntimeException(msg,f)
/**
 * A exception concerning authentication failures
 * @author max@tactix4.com
 * @param msg The exception message
 * @param f the original exception - defaults to null
 */
class OpenERPAuthenticationException(msg: String, f: Throwable = null) extends RuntimeException(msg,f)
