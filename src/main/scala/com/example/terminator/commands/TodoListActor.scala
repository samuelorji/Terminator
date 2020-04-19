package com.example.terminator.commands
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.terminator.processor.BrainMinion
import commands.{CommandProcessAnalytics, CommandWorker, CommandsMessage, HandleCommand}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object TodoListActor extends CommandWorker with CommandProcessAnalytics {
  override def commandRegexes: List[String] = List(
    "^todo.*"
  )

  override def commands: List[String] = List(
    "todo add <item>",
    "todo rm <itemId>",
    "todo list",
    "todo clear"

  )

  private [this] val todos = ArrayBuffer.empty[String]

  //(creates an in memory todo list)
  override def commandTitle: String = "todo"

  private def formatSeq(elems : Seq[String]) : String = {
    elems.zipWithIndex.foldLeft("") { case (acc, (elem, ind)) => s"$acc\n$ind: $elem" }
  }
  def apply(): Behavior[CommandsMessage] = Behaviors.setup { context =>
    implicit val system = context.system
    val logger = context.log

    setLoggerAndEc(logger, system.executionContext)
    Behaviors.receiveMessage {
      case HandleCommand(cmd, worker) =>
        processCommand(cmd) { command =>

          command.trim.split(" ") match {
            case Array("todo") | Array("todo", "list") =>
              val todoList = todos.isEmpty match {
                case true => "No Todo"
                case false =>
                 formatSeq(todos)
              }
              worker ! BrainMinion.CommandHandled(todoList)

            case Array("todo", "rm", id) if id.forall(_.isDigit) =>
              todos.isEmpty match {
                case true =>
                  worker ! BrainMinion.CommandNotHandled(s"Todo list is empty")

                case false =>
                  Try {  todos(id.toInt) }.toOption match {
                    case Some(elem) =>
                      todos.remove(id.toInt)
                      println(todos.mkString(":"))
                      worker ! BrainMinion.CommandHandled(s"'$elem' removed from todo list")
                    case None =>
                      worker ! BrainMinion.CommandNotHandled(s"No todo found for index $id")
                  }
              }


            case Array("todo","add", rest @ _*) =>
              todos.append(rest.mkString(" "))
              worker ! BrainMinion.CommandHandled(s"'${rest.mkString(" ")}' added to todo list")


            case Array("todo","clear") =>
              todos.clear()
              worker ! BrainMinion.CommandHandled(s"Todo list cleared")

            case _ =>  worker ! BrainMinion.CommandNotHandled(s"cannot understand command '$cmd'")
          }
        }
        Behaviors.same
    }

  }
}
