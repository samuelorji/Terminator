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


  //(open search query in a new chrome window)
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
              val os = System.getProperty("os.name")
                .toLowerCase()
              val errorMsg = Array.ofDim[Byte](1 * 1024) //1MB

              Try {
                if (os.contains("linux")) {
                  new ProcessBuilder("/usr/bin/google-chrome","--new-window", s"$completeQuery").start()
                } else if(os.contains("mac")) {
                  new ProcessBuilder("open", "-na", s"Google Chrome", "--args", "--new-window", s"$completeQuery").start()
                }
                else{
                  throw new Exception("We do not support windows yet ): ")
                }
              }.toEither match {
                case Left(err) =>
                  logger.error(s"Error starting process for command $cmd, error : ${err.getMessage}")
                  worker ! BrainMinion.CommandNotHandled("Can't process Google command")
                case Right(process) =>

                  println(process.isAlive)
                  val read = process.getErrorStream.read(errorMsg)
                  if (read < 0) {
                    //no error message
                    worker ! BrainMinion.CommandHandled("New Chrome Tab opened")
                  } else {
                    val error = new String(errorMsg.take(read))
                    logger.error(s"error occurred while processing [$cmd], error : $error")
                    worker ! BrainMinion.CommandNotHandled("Can't process Google command, Chrome likely not installed")
                  }
              }

            case _ =>  worker ! BrainMinion.CommandNotHandled(s"cannot understand command '$cmd'")


          }
        }
            Behaviors.same
    }

  }
}
