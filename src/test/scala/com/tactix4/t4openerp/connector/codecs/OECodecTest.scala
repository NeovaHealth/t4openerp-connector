package com.tactix4.t4openerp.connector.codecs

import scala.Some
import com.tactix4.t4openerp.connector._
import org.scalacheck.Gen
import org.scalacheck.Arbitrary._
import org.scalatest.{FunSuite, ShouldMatchers}
import org.scalatest.prop.PropertyChecks

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 12/03/14
 * Time: 16:33
 * To change this template use File | Settings | File Templates.
 */
class OECodecTest extends FunSuite with PropertyChecks with ShouldMatchers{

  lazy val genMap: Gen[Map[String, List[Option[Double]]]] = Gen.sized{ size =>
    for {
      k <- Gen.listOfN(size,arbString.arbitrary)
      v <- arbContainer[List,List[Option[Double]]].arbitrary
    } yield (k zip v).toMap
  }


  import GeneratedDecodeOE._

  import GeneratedEncodeOE._
  case class CodecTestClass(a: Int, b: Double, c: Boolean, d:String, e:Option[Int],f:Option[Double],g:Option[String],h:Option[Boolean],i:List[Int],j:Map[String,List[Option[Double]]])

  implicit val DecodeTC : OEDataDecoder[CodecTestClass] = decode10M(CodecTestClass(_:Int,_:Double,_:Boolean,_:String,_:Option[Int],_:Option[Double],_:Option[String],_:Option[Boolean],_:List[Int], _:Map[String,List[Option[Double]]]))("a","b","c","d","e","f","g","h","i","j")
  implicit val EncodeTC : OEDataEncoder[CodecTestClass] = encode10M((c:CodecTestClass) => (c.a,c.b,c.c,c.d,c.e,c.f,c.g,c.h,c.i,c.j))("a","b","c","d","e","f","g","h","i","j")

  def CodecTestClassGen  =  for {
    a <- arbInt.arbitrary
    b <- arbDouble.arbitrary
    c <- arbBool.arbitrary
    d <- arbString.arbitrary
    e <- arbOption[Int].arbitrary
    f <- arbOption[Double].arbitrary
    g <- arbOption[String].arbitrary
    h <- arbOption[Boolean].arbitrary
    i <- arbContainer[List,Int].arbitrary
    j <- genMap
  } yield CodecTestClass(a,b,c,d,e,f,g,h,i,j)



  test("Test encoding the decoding"){

    forAll(CodecTestClassGen){ randomClass =>
      val encodedClass = randomClass.encode
      val decodedClass = encodedClass.flatMap(_.decodeAs[CodecTestClass])

      decodedClass.toOption shouldEqual Some(randomClass)

    }
  }


}
