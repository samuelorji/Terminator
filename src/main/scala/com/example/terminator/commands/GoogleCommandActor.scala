package com.example.terminator.commands

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.terminator.processor.BrainMinion
import commands.{CommandProcessAnalytics, CommandWorker, CommandsMessage, HandleCommand}

import scala.util.Try

object GoogleCommandActor extends CommandWorker with CommandProcessAnalytics {
  override def commandRegexes: List[String] = List(
    "^google.*"
  )

  override def commands: List[String] = List(
    "google <search_query>"
  )


  override def commandTitle: String = "google"

  def apply(): Behavior[CommandsMessage] = Behaviors.setup{context =>
    implicit val system =  context.system
    val logger = context.log

    setLoggerAndEc(logger,system.executionContext)

    Behaviors.receiveMessage{
      case HandleCommand(cmd, worker) =>
        processCommand(cmd) { command =>
          command.trim.split(" ") match {
            case Array("google") =>
              worker ! BrainMinion.CommandNotHandled("no search query")
              Behaviors.same

            case Array("google", rest @ _*) =>
              val baseUrl = "https://www.google.com/search?q="
              val encodedQuery = URLEncoder.encode(rest.mkString(" "), StandardCharsets.UTF_8.toString)
              val completeQuery = s"$baseUrl$encodedQuery"
              val isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows")
              val errorMsg = Array.ofDim[Byte](1 * 1024) //1MB

              Try {
                if (isWindows) {
                  new ProcessBuilder("start", "-chrome", "--new-window", s"$completeQuery").start()
                } else {
                  new ProcessBuilder("open", "-na", s"Google Chrome", "--args", "--new-window", s"$completeQuery").start()

                }
              }.toEither match {
                case Left(err) =>
                  logger.error(s"Error starting process for command $cmd, error : ${err.getMessage}")
                  worker ! BrainMinion.CommandNotHandled("Can't process Google command")
                case Right(process) =>
                  val read = process.getErrorStream.read(errorMsg)
                  if (read < 0) {
                    //no error message
                    worker ! BrainMinion.CommandHandled("New Chrome Tab opened")
                  } else {
                    worker ! BrainMinion.CommandNotHandled("Can't process Google command")
                  }
              }

            case _ =>  worker ! BrainMinion.CommandNotHandled(s"cannot understand command $cmd")


          }
        }
            Behaviors.same
    }

  }
}
