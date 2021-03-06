/*
 * Copyright (C) 2014 Romain Reuillon
 * Copyright (C) 2017 Jonathan Passerat-Palmbach
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

package gridscale.tools.shell

/**
 * Ugly attempt to "generify" user shell
 * assumption 1: Bash is installed on the target host
 * assumption 2: the env variables required for a successful execution are defined in .bashrc
 * => env -i + the sourced files mimics what happens when a bash login shell is started
 * => bash << EOF prevents the default shell (which might not be bash) to process the command
 * resulting in this combination...
 */
object BashShell {

  val shell = "env -i bash"

  def remoteBashCommand(from: String): String =
    s"""$shell <<EOF
        |${buildCommand(from)}
        |EOF
        |""".stripMargin

  def localBashCommand(from: String): (String, String) =
    (shell, buildCommand(from))

  def source: String =
    s"""
       |source /etc/profile 2>/dev/null
       |source ~/.bash_profile 2>/dev/null
       |source ~/.bash_login 2>/dev/null
       |source ~/.profile 2>/dev/null
       |""".stripMargin

  def buildCommand(cmd: String): String =
    s"""
     |$source
     |$cmd
     |""".stripMargin
}
