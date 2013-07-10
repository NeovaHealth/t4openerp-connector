import scala.collection.mutable

/**
 * Created by max@tactix4.com
 * 5/20/13
 */


class OpenERPContext(val ActiveTest: Boolean = true,
              val Lang: String,
              val Timezone: String,
              val Uid:Int) extends mutable.HashMap[String, Any] {

  val ActiveTestTag = "active_test"
  val TimezoneTag = "tz"
  val LangTag = "lang"
  val UidTag = "uid"

  def putAll(values: Map[String, Any]) {
    values map (s => put(s._1, s._2))
  }

  def getUid = get(UidTag).asInstanceOf[Int]
  def getActiveTest = get(ActiveTestTag).asInstanceOf[Boolean]
  def getTimeZone = get(TimezoneTag).asInstanceOf[String]
  def getLanguage = get(LangTag).asInstanceOf[String]

  def setActiveTest(b: Boolean) = put(ActiveTestTag,b)
  def setTimeZone(tz: String) = put(TimezoneTag, tz)
  def setLanguage(l: String) = put(LangTag, l)
  def setUid(id:Int) = put(UidTag, id)


}
