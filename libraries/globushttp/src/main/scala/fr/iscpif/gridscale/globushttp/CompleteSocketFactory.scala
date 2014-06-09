/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package fr.iscpif.gridscale.globushttp

import org.apache.commons.httpclient.protocol.ProtocolSocketFactory
import org.globus.gsi.gssapi.auth.HostOrSelfAuthorization
import org.gridforum.jgss.{ExtendedGSSContext, ExtendedGSSManager}
import org.globus.gsi.gssapi.GSSConstants
import org.ietf.jgss.GSSContext
import org.globus.gsi.GSIConstants
import org.globus.gsi.gssapi.net.{GssSocket, GssSocketFactory}
import java.net.{Socket, InetAddress}
import org.apache.commons.httpclient.params.HttpConnectionParams

trait CompleteSocketFactory <: SocketFactory {
  def proxyBytes: Array[Byte]
  def timeout: Int

  def factory = new ProtocolSocketFactory {
    def getSocket(host: String, port: Int, timeout: Int = 10000) = {
      val authorisation = HostOrSelfAuthorization.getInstance()
      val manager = ExtendedGSSManager.getInstance

      // Expected name is null since delegation is never
      // done. Custom authorization is invoked after handshake is finished.
      val context = manager.createContext(null,
        GSSConstants.MECH_OID,
        GlobusHttpClient.credential(proxyBytes),
        GSSContext.DEFAULT_LIFETIME).asInstanceOf[ExtendedGSSContext]


      context.setOption(GSSConstants.GSS_MODE, GSIConstants.MODE_SSL)
      context.requestConf(false)

      val socket =
        javax.net.SocketFactory.getDefault.createSocket(host, port)

      val gsiSocket =
        GssSocketFactory.getDefault.createSocket(socket,
          host,
          port,
          context).asInstanceOf[GssSocket]

      gsiSocket.setAuthorization(authorisation)

      gsiSocket
    }


    def createSocket(host: String, port: Int): java.net.Socket = getSocket(host, port)

    def createSocket(host: String, port: Int, localAddress: InetAddress, localPort: Int, params: HttpConnectionParams): java.net.Socket = getSocket(host, port)

    def createSocket(host: String, port: Int, localAddress: java.net.InetAddress, localPort: Int): Socket = getSocket(host, port)
  }
}