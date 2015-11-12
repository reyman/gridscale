/**
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import fr.iscpif.gridscale.benchmark.slurm._

object Main {
  def main(argv: Array[String]): Unit = {

    val (host, username, password, privateKeyPath) = argv match {
      case Array(h, u, p, pKP)        ⇒ (h, u, p, pKP)
      case Array(h, u, p, null)       ⇒ (h, u, p, null)
      case Array(h, u, null, null)    ⇒ (h, u, null, null)
      case Array(h, null, null, null) ⇒ (h, null, null, null)
      case _                          ⇒ throw new RuntimeException("Bad arguments")
    }

    val nbJobs = 1
    val nbRuns = 2
    val b = new SlurmBenchmark(host, username, password, privateKeyPath)(nbJobs)
    val (avgSubmit, avgQuery, avgCancel) = b.avgBenchmark(nbRuns).toList match {
      case List(a, b, c) ⇒ (a, b, c)
    }

    println(
      s"""Average for ${nbJobs} jobs along ${nbRuns} runs (milliseconds):
         |\tsubmit: ${avgSubmit}
         |\tstate: ${avgQuery}
         |\tcancel: ${avgCancel}
       """.stripMargin)
  }
}
