/*
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
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

package fr.iscpif.gridscale.benchmark
package pbs

import java.io.File
import fr.iscpif.gridscale.ssh._
import fr.iscpif.gridscale.pbs._
import fr.iscpif.gridscale.benchmark.util._

import scala.concurrent.duration._

class PBSBenchmark(val inHost: String, val inUsername: String, val inPassword: String, val inPrivateKeyPath: String)(val nbJobs: Int)
    extends Benchmark with PBSJobService with SSHPrivateKeyAuthentication { slurmService ⇒

  override val jobDescription = new PBSJobDescription {
    val executable = slurmService.executable
    val arguments = slurmService.arguments
    val workDirectory = "/work/jpassera/benchmark"
    override val wallTime = Some(30 minutes)
  }

  def host = inHost
  def user = inUsername
  def password = inPassword
  def privateKey = new File(inPrivateKeyPath)
}
