import akka.actor.typed.ActorRef
import com.example.terminator.processor.BrainMinion.MinionMessage
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

package object commands {
  trait CommandsMessage
  final case class HandleCommand(command : String,replyTo : ActorRef[MinionMessage]) extends CommandsMessage

  trait CommandWorker {
    def commandRegexes: List[String]
    def commands: List[String]
    def commandTitle : String
  }
  private def processingTime(start : Long) = System.currentTimeMillis() - start


  trait CommandProcessAnalytics {
    private var log : Option[Logger] = None
    private var exC : Option[ExecutionContext] = None

    def setLoggerAndEc(logger : Logger , ec : ExecutionContext) = {
      log = Some(logger)
      exC = Some(ec)

    }
    def processCommand(cmd : String)(fun : String => Unit) : Unit = {
      if(log.isEmpty && exC.isEmpty){
        throw new IllegalArgumentException("Logger and Execution context not set")
      }else {
        val startTime = System.currentTimeMillis()
        fun(cmd)
        log.foreach(_.info(s"Processing of command [$cmd] completed after ${processingTime(startTime)} ms"))
      }

    }

    def processCommandAsync(cmd : String)(fun : String => Future[Unit]) : Future[Unit] = {
      if(log.isEmpty && exC.isEmpty){
        throw new IllegalArgumentException("Logger and Execution context not set")
      }else {

        implicit val ec = exC.get
        val startTime = System.currentTimeMillis()
        fun(cmd)
          .map(_ =>  log.get.info(s"Processing of $cmd completed after ${processingTime(startTime)} ms"))
          .recover{ case ex => log.get.error(s"Processing of [$cmd] failed after ${processingTime(startTime)} ms",ex) }
      }

    }
  }



}
