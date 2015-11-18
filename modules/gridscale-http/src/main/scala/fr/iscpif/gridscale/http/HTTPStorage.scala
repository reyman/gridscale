/*
 * Copyright (C) 2012 Romain Reuillon
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

package fr.iscpif.gridscale.http

import java.io.{ File, InputStream, OutputStream }
import java.net.{ HttpURLConnection, URI }

import fr.iscpif.gridscale._
import fr.iscpif.gridscale.storage._
import fr.iscpif.gridscale.tools.{ _ }
import org.htmlparser.Parser
import org.htmlparser.filters.NodeClassFilter
import org.htmlparser.tags.LinkTag

import scala.concurrent.duration._

object HTTPStorage {

  def withConnection[T](uri: URI, timeout: Duration)(f: HttpURLConnection ⇒ T): T = {
    val relativeURL = uri.toURL
    val cnx = relativeURL.openConnection.asInstanceOf[HttpURLConnection]
    cnx.setConnectTimeout(timeout.toMillis.toInt)
    cnx.setReadTimeout(timeout.toMillis.toInt)
    if (cnx.getHeaderField(null) == null) throw new RuntimeException("Failed to connect to url: " + relativeURL)
    else f(cnx)
  }

  def apply(url: String, timeout: Duration = 1 minute) = {
    val (_url, _timeout) = (url, timeout)
    new HTTPStorage {
      override val url: String = _url
      override val timeout = _timeout
    }
  }

}

trait HTTPStorage extends Storage {

  def url: String
  def bufferSize = 64000
  def timeout: Duration

  def _list(path: String) = {
    val is = openInputStream(path)
    try {
      val parser = new Parser
      parser.setInputHTML(new String(getBytes(is, bufferSize, timeout)))
      val list = parser.extractAllNodesThatMatch(new NodeClassFilter(classOf[LinkTag]))

      list.toNodeArray.flatMap {
        l ⇒
          val entryName = l.getText.substring("a href=\"".size, l.getText.size - 1)
          val isDir = entryName.endsWith("/")
          val name = if (isDir) entryName.substring(0, entryName.length - 1) else entryName
          if (!name.isEmpty && !name.contains("/") && !name.contains("?") && !name.contains("#")) {
            val ret = name.replaceAll("&amp;", "%26")
            Some(
              ListEntry(
                new File(java.net.URLDecoder.decode(ret, "utf-8")).getPath,
                if (isDir) DirectoryType else FileType,
                None
              )
            )
          } else None
      }
    } finally is.close
  }

  def _makeDir(path: String) =
    throw new RuntimeException("Operation not supported for http protocol")

  def _rmDir(path: String) =
    throw new RuntimeException("Operation not supported for http protocol")

  def _rmFile(patg: String) =
    throw new RuntimeException("Operation not supported for http protocol")

  def _mv(from: String, to: String) =
    throw new RuntimeException("Operation not supported for http protocol")

  protected def _openInputStream(path: String): InputStream = withConnection(path) {
    _.getInputStream
  }

  protected def _openOutputStream(path: String): OutputStream =
    throw new RuntimeException("Operation not supported for http protocol")

  private def withConnection[T](path: String)(f: HttpURLConnection ⇒ T): T =
    HTTPStorage.withConnection[T](new URI(url + "/" + path), timeout)(f)

}
