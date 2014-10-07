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

package fr.iscpif.gridscale.ssh

import java.util.UUID
import java.util.logging.Logger
import fr.iscpif.gridscale.tools.shell._
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.common.IOUtils
import fr.iscpif.gridscale._
import tools._
import jobservice._

import scala.util.Try

object SSHJobService {

  // val rootDir = ".gridscale/ssh"

  def file(dir: String, jobId: String, suffix: String) = dir + "/" + jobId + "." + suffix
  def pidFile(dir: String, jobId: String) = file(dir, jobId, "pid")
  def endCodeFile(dir: String, jobId: String) = file(dir, jobId, "end")
  def outFile(dir: String, jobId: String) = file(dir, jobId, "out")
  def errFile(dir: String, jobId: String) = file(dir, jobId, "err")

  //val PROCESS_CANCELED = 143
  //val COMMAND_NOT_FOUND = 127

  /*def exec (connection: Connection, cde: String): Unit = {
    val session = connection.openSession
    try {
      exec(session, cde) 
      if(session.getExitStatus != 0) throw new RuntimeException("Return code was no 0 but " + session.getExitStatus)
    } finally session.close
  } */

  def withSession[T](c: SSHClient)(f: Session ⇒ T): T = {
    val session = c.startSession
    try f(session)
    finally session.close
  }

  def execReturnCode(cde: Command)(implicit client: SSHClient) = withSession(client) { session ⇒
    val cmd = session.exec(cde.toString)
    try {
      cmd.join
      cmd.getExitStatus
    } finally cmd.close
  }

  def execReturnCodeOutput(cde: Command)(implicit client: SSHClient) = withSession(client) { session ⇒
    val cmd = session.exec(cde.toString)
    try {
      cmd.join
      (cmd.getExitStatus, IOUtils.readFully(cmd.getInputStream).toString, IOUtils.readFully(cmd.getErrorStream).toString)
    } finally cmd.close
  }

  def exec(cde: Command)(implicit client: SSHClient) = withSession(client) { session ⇒
    val retCode = execReturnCode(cde)
    if (retCode != 0) throw new RuntimeException("Return code was no 0 but " + retCode + " while executing " + cde)
  }

  def launch(cde: Command)(implicit client: SSHClient) = withSession(client) {
    _.exec(cde.toString).close
  }

  def exception(ret: Int, command: String, output: String, error: String) = new RuntimeException(s"Unexpected return code $ret, when running $command (stdout=$output, stderr=$error")

  case class JobId(jobId: String, workDirectory: String)
}

import SSHJobService._

trait SSHJobService extends JobService with SSHHost with SSHStorage with BashShell { js ⇒
  type J = JobId
  type D = SSHJobDescription

  def bufferSize = 65535

  def toScript(description: D, background: Boolean = true) = {
    val jobId = UUID.randomUUID.toString
    val command = new ScriptBuffer

    def absolute(path: String) = description.workDirectory + "/" + path

    //command += "mkdir -p " + absolute(rootDir)
    command += "mkdir -p " + description.workDirectory
    command += "cd " + description.workDirectory

    val executable = description.executable + " " + description.arguments

    val jobDir =
      command += s"((" +
        executable +
        " >" + outFile(description.workDirectory, jobId) + " 2>" + errFile(description.workDirectory, jobId) + " ; " +
        " echo $? >" + endCodeFile(description.workDirectory, jobId) + s") ${if (background) "&" else ";"} " +
        "echo $! >" + pidFile(description.workDirectory, jobId) + " )"

    (command.toString, jobId)
  }

  def execute(description: D) = withConnection { implicit c ⇒
    val (command, jobId) = toScript(description, background = false)
    val (ret, out, err) = execReturnCodeOutput(command)
    try if (ret != 0) throw exception(ret, command, out, err)
    finally { purge(JobId(jobId, description.workDirectory)) }
  }

  def submit(description: D): J = {
    val (command, jobId) = toScript(description)
    withConnection(launch(command)(_))
    JobId(jobId, description.workDirectory)
  }

  def state(job: J): JobState =
    if (jobIsRunning(job)) Running
    else {
      if (exists(endCodeFile(job.workDirectory, job.jobId))) {
        val is = openInputStream(endCodeFile(job.workDirectory, job.jobId))
        val content =
          try getBytes(is, bufferSize, timeout)
          finally is.close

        translateState(new String(content).takeWhile(_.isDigit).toInt)
      } else Failed
    }

  def cancel(job: J) = withConnection { implicit connection ⇒
    val cde = s"kill `cat ${pidFile(job.workDirectory, job.jobId)}`;"
    exec(cde)
  }

  def purge(job: J) = withConnection { implicit connection ⇒
    val cde = s"rm -rf ${job.workDirectory}/${job.jobId}*"
    exec(cde)
  }

  private def jobIsRunning(job: J) = {
    val cde = s"ps -p `cat ${pidFile(job.workDirectory, job.jobId)}`"
    withConnection(execReturnCode(cde)(_) == 0)
  }

  private def translateState(retCode: Int) =
    retCode match {
      case 0 ⇒ Done
      case _ ⇒ Failed
    }

}
