package com.tactix4.openerpConnector.transport

import com.tactix4.simpleXmlRpc._
import com.tactix4.simpleXmlRpc.XmlRpcInt
import com.typesafe.scalalogging.log4j.Logging
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success, Try}
import ExecutionContext.Implicits.global
import com.tactix4.openerpConnector.OpenERPSession

/**
 * @author max@tactix4.com
 *         7/12/13
 */
object XmlRpcTransportAdaptor extends OpenERPTransportAdaptor with Logging{


   implicit object XmlRpcDataTypeFormat extends TransportDataFormat[XmlRpcDataType]{

     def write(obj: XmlRpcDataType): TransportDataType = obj match{
       case x: XmlRpcInt     => TransportNumber(x.value)
       case x: XmlRpcDouble  => TransportNumber(x.value.toDouble)
       case x: XmlRpcString  => TransportString(x.value)
       case x: XmlRpcBoolean => TransportBoolean(x.value)
       case x: XmlRpcBase64  => TransportString(x.value.toString)
       case x: XmlRpcArray   => TransportArray(x.value.map(write))
       case x: XmlRpcStruct  => TransportMap(x.value.map((t: (String, XmlRpcDataType)) => {
         TransportString(t._1) -> write(t._2)
       }))
       case x: XmlRpcDateTime => TransportString(x.value.toString)
     }

     def read(obj: TransportDataType): XmlRpcDataType = obj match {
       case TransportNumber(x:Int) => XmlRpcInt(x)
       case TransportNumber(x) => XmlRpcDouble(x.asInstanceOf[Double])
       case TransportString(x) => XmlRpcString(x)
       case TransportBoolean(x) => XmlRpcBoolean(x)
       case TransportArray(x) => XmlRpcArray(x.map(read))
       case TransportMap(x) => XmlRpcStruct(x.map((t: (TransportString, TransportDataType)) => (t._1.value, read(t._2))))
       case TransportNull(x) => XmlRpcBoolean(false)
     }
   }



   override def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: List[TransportDataType]): Future[TransportResponse] = {
    logger.info("sending request with\nconfig: " + config + "\nmethodName: " + methodName + "\nparams: " + params)

    val xmlRpcConfig = XmlRpcConfig(RPCProtocol.stringToRpcProtocol(config.protocol), config.host, config.port, config.path, config.headers)
    val answer = XmlRpcClient.request(xmlRpcConfig, methodName, params.toList.map(_.convertTo[XmlRpcDataType]))
    val promise = Promise[TransportResponse]()

    answer.onComplete(
      result => result match{
        case Success(r) => r match {
          case s: XmlRpcResponseNormal =>  promise.complete(Try(Right(s.params.headOption.map(x => x.toTransportDataType).getOrElse(TransportArray(Nil)))))
          case s: XmlRpcResponseFault  =>  promise.complete(Try(Left(s.toString)))
        }
        case Failure(e) => promise.failure(e)
      }
    )
    promise.future
  }



 }
