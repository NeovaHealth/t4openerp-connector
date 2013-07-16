package com.tactix4.openerpConnector

import scala.collection.mutable
import com.tactix4.openerpConnector.transport._
import com.tactix4.simpleXmlRpc.XmlRpcDataType

/**
 * Created by max@tactix4.com
 * 5/20/13
 */


class OpenERPContext(activeTest: Boolean = true,
              lang: String,
              timezone: String) extends mutable.HashMap[String, Either[String,Boolean]] {

  val ActiveTestTag = "active_test"
  val TimezoneTag = "tz"
  val LangTag = "lang"

  setActiveTest(activeTest)
  setTimeZone(timezone)
  setLanguage(lang)

  def putAll(values: Map[String, Either[String, Boolean]]) {
    values map (s => put(s._1, s._2))
  }
  def getActiveTest = get(ActiveTestTag)
  def getTimeZone = get(TimezoneTag)
  def getLanguage = get(LangTag)

  def setActiveTest(b: Boolean) = put(ActiveTestTag,Right(b))
  def setTimeZone(tz: String) = put(TimezoneTag, Left(tz))
  def setLanguage(l: String) = put(LangTag, Left(l))



}

object OpenERPContext{

  def apply(at: Boolean, lang: String, timezone: String) = new OpenERPContext(at,lang,timezone)

  implicit object OpenERPContextFormat extends TransportDataFormat[OpenERPContext]{
    def write(obj: OpenERPContext): TransportDataType =
      TransportMap(obj.toList.map(t => TransportString(t._1) -> t._2.fold(TransportString, TransportBoolean)))

    def read(obj: TransportDataType): OpenERPContext = ???

}}
