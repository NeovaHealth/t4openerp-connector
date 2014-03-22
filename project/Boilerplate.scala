import sbt._

object Boilerplate {
  val arities = (1 to 22)
  val aritiesExceptOne = (2 to 22)
  val arityChars: Map[Int, Char] = arities.map(n => (n, ('A' + n - 1).toChar)).toMap

  def write(path: File, fileContents: String): File = {
    IO.write(path, fileContents)
    path
  }

  def gen(dir: File) = {
    val generatedDecodeOE = write(dir / "com" / "tactix4" / "t4openerp-connector" / "GeneratedDecodeOE.scala", genDecodeOE)

    val generatedEncodeOE = write(dir / "com" / "tactix4" / "t4openerp-connector" / "GeneratedEncodeOE.scala", genEncodeOE)
    //
    //    val generatedCodecJson = write(dir / "argonaut" / "GeneratedCodecJsons.scala", genCodecJsons)

    Seq(generatedDecodeOE, generatedEncodeOE) //, generatedCodecJson)
  }

  def header = {
    """|
      |package com.tactix4.t4openerp.connector.codecs
      |import com.tactix4.t4openerp.connector.transport.OENull
      |import scalaz._
      |import Scalaz._
      |
      | """.stripMargin
  }

  def functionTypeParameters(arity: Int): String = (1 to arity).map(n => arityChars(n)).mkString(", ")

  def tupleFields(arity: Int): String = (1 to arity).map(n => "x._" + n).mkString(", ")

  def listPatternMatch(arity: Int): String = ((1 to arity).map(n => "c" + arityChars(n).toLower).toList ::: "Nil" :: Nil).mkString(" :: ")

  def jsonStringParams(arity: Int): String = (1 to arity).map(n => "%sn: String".format(arityChars(n).toLower)).mkString(", ")

  def tuples(arity: Int): String = (1 to arity).map(n => "%sn -> t._%d.encode".format(arityChars(n).toLower, n)).mkString(", ")

  def encodeTupleValues(arity: Int): String = (1 to arity).map(n => "t._%d.encode".format(n)).mkString(", ")

  def jsonStringParamNames(arity: Int): String = (1 to arity).map(n => "%sn".format(arityChars(n).toLower)).mkString(", ")

 def genEncodeOE = {
    def encodeOEContextArities(n: Int): String = (1 to n).map(n => "%s: OEDataEncoder".format(arityChars(n))).mkString(", ")

    def content = {
      val encode1M =
        """|
          |
          | def encode1M[A:OEDataEncoder,X](f: X => A)(an: String): OEDataEncoder[X] =
          |   OEDataEncoder[X](x => {
          |     val t = f(x).encode
          |     t.map(e => OEMap(an -> e) )
          |   })
          | """.stripMargin


      val encodeMs = aritiesExceptOne.map {
        arity =>
          """|
            |  def encode%sM[%s, X](f: X => (%s))(%s): OEDataEncoder[X] =
            |     OEDataEncoder[X](x => {
            |       val t = f(x)
            |       val e = List(%s).sequence[CodecResult,OEType]
            |       e.map(l => OEMap(List(%s) zip l toMap))
            |     })
            | """.format(
            arity,
            encodeOEContextArities(arity),
            functionTypeParameters(arity),
            jsonStringParams(arity),
            encodeTupleValues(arity),
            jsonStringParamNames(arity)
          ).stripMargin
      }

      (encode1M +: encodeMs).mkString
    }

   header +
     """|
       |import com.tactix4.t4openerp.connector.transport.OEMap
       |import com.tactix4.t4openerp.connector.transport.OEType
       |import com.tactix4.t4openerp.connector.{CodecResult, pimpEncoder}
       |import scala.language.postfixOps
       |
       |object GeneratedEncodeOE {
       |%s
       |}
       | """.format(content).stripMargin
 }

  def genDecodeOE = {
    def decodeOEContextArities(n: Int): String = (1 to n).map(n => "%s: OEDataDecoder".format(arityChars(n))).mkString(", ")

    def content = {
      val jdecode1L =
        """|
          |  def decode1M[A: OEDataDecoder, X](f: A => X)(an: String): OEDataDecoder[X] =
          |    OEDataDecoder(c => {
          |     val r = c.dict.map(d => for {
          |      f1 <- (d.get(an) | OENull).decodeAs[A]
          |     } yield f(f1))
          |     r | s"Unable to decode: $c".failure
          |     })
          |
          | """.stripMargin


      val jdecodeLs = aritiesExceptOne.map {
        arity =>


          val secondForComprehensionLines: String = (1 to arity).map {
            n =>
              val upperChar = arityChars(n)
              val lowerChar = upperChar.toLower
              "       f%d <- (d.get(%sn) | OENull).decodeAs[%s]".format(n, lowerChar,upperChar)
          }.mkString("\n")


          val secondYieldExpression: String = (1 to arity).map {
            n =>
              "f%d".format(n)
          }.mkString(", ")

          """|
            |  def decode%sM[%s, X](f: (%s) => X)(%s): OEDataDecoder[X] =
            |    OEDataDecoder(c => {
            |     val s = c.dict.map( d => for {
            |%s
            |     } yield f(%s))
            |
            |     s | s"Unable to decode $c".failure
            |   })
            | """.format(
            arity,
            decodeOEContextArities(arity),
            functionTypeParameters(arity),
            jsonStringParams(arity),
            secondForComprehensionLines,
            secondYieldExpression
          ).stripMargin
      }

      (jdecode1L +: jdecodeLs).mkString
    }

    header +
      """|
        |import com.tactix4.t4openerp.connector.pimpDecoder
        |
        |object GeneratedDecodeOE {
        |%s
        |}
        | """.format(content).stripMargin
  }
}

