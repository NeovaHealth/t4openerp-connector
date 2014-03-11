package com.tactix4.t4openerp.connector.codecs

import scalaz._
import Scalaz._
import com.tactix4.t4openerp.connector.pimpDecoder
import com.tactix4.t4openerp.connector.transport.{OEMap, OEType}

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 11/03/14
 * Time: 09:25
 * To change this template use File | Settings | File Templates.
 */
object GeneratedDecodeOETEST {

  import com.tactix4.t4openerp.connector._

 def encode4M[A:OEDataEncoder, B:OEDataEncoder, C:OEDataEncoder, D:OEDataEncoder, X](f: X => (A, B, C, D) )(an: String, bn: String, cn: String, dn: String): OEDataEncoder[X] =
    OEDataEncoder[X](x => OEMap((List(an,bn,cn, dn) zip (f(x) map (_.encode)).toList).toMap))


   //  def encode1M2[A,X](f: OEType => A)(an:String)(implicit ev: OEDataEncoder[A]) : OEDataEncoder[X] =
  def decode1M2[A, X](f: A => X)(an: String)(implicit ev: OEDataDecoder[A]): OEDataDecoder[X] =
    OEDataDecoder(c => {
     val r = for {
      x <- c.dict
      aa <- x.get(an)
     } yield aa
     r.map(z => ev.decode(z) map f) | DecodeResult.fail("Unable to decode")
     }
   )

//  def decode1M[A: OEDataDecoder, X](f: A => X)(an: String): OEDataDecoder[X] =
//    OEDataDecoder(x => {
//      val r = for {
//        m <- x.dict
//        aa <- m.get(an)
//      } yield aa
//      r.map(_.decodeAs[A].map(f)) | DecodeResult.fail("Unable to decode")
//    })
//
  def decode2M[A: OEDataDecoder, B:OEDataDecoder, X](f: (A, B) => X)(an: String, bn: String): OEDataDecoder[X] =
    OEDataDecoder(x => {
      val r = for {
        m <- x.dict
        aa <- m.get(an)
        bb <- m.get(bn)
      } yield (aa,bb)

      val s = r.map(t => for {
        f1 <- implicitly[OEDataDecoder[A]].decode(t._1)
        f2 <- implicitly[OEDataDecoder[B]].decode(t._2)
      } yield f(f1,f2))

      s | DecodeResult.fail("Unable to decode")
    })
//
//  def decode3M[A: OEDataDecoder, B:OEDataDecoder,C:OEDataDecoder, X](f: (A, B, C) => X)(an: String, bn: String, cn:String): OEDataDecoder[X] =
//    OEDataDecoder(x => {
//      val r = for {
//        m <- x.dict
//        aa <- m.get(an)
//        bb <- m.get(bn)
//        cc <- m.get(cn)
//      } yield (aa,bb,cc)
//
//      val s = r.map( t => for {
//        f1 <- t._1.decodeAs[A]
//        f2 <- t._2.decodeAs[B]
//        f3 <- t._3.decodeAs[C]
//      } yield f(f1,f2,f3))
//
//      s | DecodeResult.fail("Unable to decode")
//
//    })
}
