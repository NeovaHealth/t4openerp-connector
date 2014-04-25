package com.tactix4.t4openerp.connector.transport

import org.scalatest.FunSuite
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck.{Choose, Arbitrary, Gen}
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._
import com.tactix4.t4xmlrpc._

/**
 * Created by max on 23/04/14.
 */

object XmlRpcGen{

  def oneXmlType(depth:Int) = Gen.lzy{Gen.oneOf(intGen,doubleGen,stringGen,boolGen,base64Gen,dateGen,arrayGen(depth), structGen(depth))}
  lazy val oneNRXmlType = Gen.oneOf(intGen,doubleGen,stringGen,boolGen,base64Gen,dateGen)


  val intGen : Gen[XmlRpcInt]  = arbInt.arbitrary.map(XmlRpcInt)
  val doubleGen : Gen[XmlRpcDouble] = arbDouble.arbitrary.map(XmlRpcDouble)
  val stringGen : Gen[XmlRpcString] = arbString.arbitrary.map(XmlRpcString)
  val boolGen : Gen[XmlRpcBoolean]  = arbBool.arbitrary.map(XmlRpcBoolean)
  val base64Gen : Gen[XmlRpcBase64] = arbArray[Byte].arbitrary.map(XmlRpcBase64)
  val dateGen : Gen[XmlRpcDate] = arbDate.arbitrary.map(XmlRpcDate)
  def arrayGen(depth:Int) : Gen[XmlRpcArray] = {
    if(depth == 0) {
      for {
        n <- Gen.choose(0,100)
        l <- listOfN(n,oneNRXmlType).map(XmlRpcArray(_))
      } yield l
    }
    else {
      for {
        n <- Gen.choose(0,6)
        l <- listOfN(n,oneXmlType(depth-1)).map(XmlRpcArray(_))
      }yield l
    }
  }

  implicit def arbTuple(depth:Int) = for {
    s <- arbString.arbitrary
    v <- oneXmlType(depth)
  } yield (s,v)

  implicit def arbTupleNR = for {
    s <- arbString.arbitrary
    v <- oneNRXmlType
  } yield (s,v)

  def structGen(depth: Int) : Gen[XmlRpcStruct] = {
    if(depth == 0) {
      for {
        n <- Gen.choose(0,100)
        l <- listOfN(n,arbTupleNR).map(x => XmlRpcStruct(x.toMap))
      } yield l
    }
    else {
      for {
        n <- Gen.choose(0,6)
        l <- listOfN(n,arbTuple(depth-1)).map(x => XmlRpcStruct(x.toMap))
      }yield l

    }
  }

  def XmlRpcTypeGen(depth:Int): Gen[XmlRpcDataType] = oneOf(structGen(depth),arrayGen(depth))

 implicit def arbXmlRpc :Arbitrary[XmlRpcDataType] =  Arbitrary {sized( depth => XmlRpcTypeGen(depth))}

}
class XmlRpcToOETest extends FunSuite with GeneratorDrivenPropertyChecks{
 test("Ensure XML2OE encoding and decoding does not lose any precision"){
   forAll(XmlRpcGen.arbXmlRpc.arbitrary) {
     original: XmlRpcDataType => {
       val encoded = XmlRpcOEAdaptor.XmlRpcToOE.encode(original)
       val decoded = XmlRpcOEAdaptor.XmlRpcToOE.decode(encoded)
       assert(decoded.toString === original.toString)
     }
   }
 }

}
