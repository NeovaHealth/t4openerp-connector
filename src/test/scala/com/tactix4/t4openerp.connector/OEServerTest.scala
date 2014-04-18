package com.tactix4.t4openerp.connector

import com.tactix4.t4openerp.connector.transport.{OETransportConfig, OETransportAdaptor}
import org.scalatest.FunSuite
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

/**
 * Created by max on 18/04/14.
 */
class OEServerTest extends FunSuite with MockitoSugar{

  val server = mock[OETransportAdaptor]
  val config = OETransportConfig("http","somehost",9999,"/path")

  test("test") {
      when(server.sendRequest(config, "execute", Nil)).thenReturn("blagh")
    }
  }

}
