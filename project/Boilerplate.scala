import sbt._

object Boilerplate {
  val arities = (1 to 22)
  val aritiesExceptOne = (2 to 22)
  val arityChars: Map[Int, Char] = arities.map(n => (n, ('A' + n - 1).toChar)).toMap

  def write(path: File, fileContents: String): File = {
    IO.write(path, fileContents)
    path
  }

  def gen(dir : File) = {
    val generatedDecodeJson = write(dir / "com" / "tactix4"/ "t4openerp-connector" / "GeneratedDecodeOE.scala", genDecodeOE)

//    val generatedEncodeJson = write(dir / "argonaut" / "GeneratedEncodeJsons.scala", genEncodeJsons)
//
//    val generatedCodecJson = write(dir / "argonaut" / "GeneratedCodecJsons.scala", genCodecJsons)

    Seq(generatedDecodeJson)//, generatedEncodeJson, generatedCodecJson)
  }

  def header = {
    """|
      |package com.tactix4.t4openerp.connector.codecs
      |import scalaz._
      |import Scalaz._
      |import com.tactix4.t4openerp.connector.pimpDecoder
      |
      |""".stripMargin
  }

  def functionTypeParameters(arity: Int): String = (1 to arity).map(n => arityChars(n)).mkString(", ")

  def tupleFields(arity: Int): String = (1 to arity).map(n => "x._" + n).mkString(", ")

  def listPatternMatch(arity: Int): String = ((1 to arity).map(n => "c" + arityChars(n).toLower).toList ::: "Nil" :: Nil).mkString(" :: ")

  def jsonStringParams(arity: Int): String = (1 to arity).map(n => "%sn: String".format(arityChars(n).toLower)).mkString(", ")

  def jsonStringParamNames(arity: Int): String = (1 to arity).map(n => "%sn".format(arityChars(n).toLower)).mkString(", ")


  def genDecodeOE = {
    def decodeOEContextArities(n: Int): String = (1 to n).map(n => "%s: OEDataDecoder".format(arityChars(n))).mkString(", ")

    def content = {
      val jdecode1L =
        """|
          |  def decode1M[A: OEDataDecoder, X](f: A => X)(an: String): OEDataDecoder[X] =
          |    OEDataDecoder(c => {
          |     val r = for {
          |      x <- c.dict
          |      aa <- x.get(an)
          |     } yield aa
          |     r.map(z => implicitly[OEDataDecoder[A]].decode(z).map(f)) | DecodeResult.fail("Unable to decode")
          |     }
          |    )
          |""".stripMargin


      val jdecodeLs = aritiesExceptOne.map{arity =>

        val firstForComprehensionLines: String = (1 to arity).map{n =>
          val upperChar = arityChars(n)
          val lowerChar = upperChar.toLower
          "       %s%s <- x.get(%sn)".format(lowerChar, lowerChar, lowerChar)
        }.mkString("\n")

        val secondForComprehensionLines: String = (1 to arity).map{n =>
          val upperChar = arityChars(n)
          val lowerChar = upperChar.toLower
          "       f%d <- implicitly[OEDataDecoder[%s]].decode(t._%s)".format(n,upperChar, n)
        }.mkString("\n")

        val firstYieldExpression: String = (1 to arity).map{n =>
          val lowerChar = arityChars(n).toLower
          "%s%s".format(lowerChar, lowerChar)
        }.mkString(", ")

        val secondYieldExpression: String = (1 to arity).map{n =>
          "f%d".format(n)
        }.mkString(", ")

        """|
          |  def decode%sM[%s, X](f: (%s) => X)(%s): OEDataDecoder[X] =
          |    OEDataDecoder(c => {
          |     val r = for {
          |       x <- c.dict
          |%s
          |     } yield (%s)
          |
          |     val s = r.map( t => for {
          |%s
          |     } yield f(%s))
          |
          |     s | DecodeResult.fail("Unable to decode")
          |   })
          |""".format(
                  arity,
                  decodeOEContextArities(arity),
                  functionTypeParameters(arity),
                  jsonStringParams(arity),
                  firstForComprehensionLines,
                  firstYieldExpression,
                  secondForComprehensionLines,
                  secondYieldExpression
                ).stripMargin
      }

      (jdecode1L +: jdecodeLs).mkString
    }

    header +
      """|
         |object GeneratedDecodeOE {
         |%s
         |}
         |""".format(content).stripMargin
  }
  }

