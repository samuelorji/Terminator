package com.example.terminator.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.pattern.BackoffSupervisor
import com.example.terminator.Input
import com.example.terminator.Input.AppMessages
import com.example.terminator.commands.{GoogleCommandActor, HelloCommandActor, TodoListActor, WeatherCommandActor}
import com.example.terminator.output.MouthActor
import com.example.terminator.output.MouthActor.{MouthMessages, Show}
import commands.CommandWorker

object Brain {
  sealed trait BrainMessage

  case object EmptyCommand extends BrainMessage
  case object GetAllCommands extends BrainMessage
  case class InvalidCommand(command : String) extends BrainMessage
  case class ErrorProcessingCommand(errorMsg : String) extends BrainMessage
  case class ProcessCommand(cmd : String) extends BrainMessage
  case class CommandProcessed(result : String) extends BrainMessage
  case object DelayedResult extends BrainMessage

  case object Done extends BrainMessage
  case object Quit extends BrainMessage

  val workers = List(HelloCommandActor,GoogleCommandActor,WeatherCommandActor,TodoListActor)

  private def formatCommand(worker : CommandWorker) : String = {
    s"${worker.commandTitle}:\n\t${worker.commands.mkString("\n\t")}"
  }

  def apply(output : ActorRef[MouthMessages], input : ActorRef[AppMessages])
  : Behavior[BrainMessage]
  = Behaviors.setup{ context =>


    val helloActor = context.spawn(HelloCommandActor(),"HelloActor")
    val weatherActor = context.spawn(WeatherCommandActor(),"weather")
    val googleActor = context.spawn(GoogleCommandActor(),"google")
    val todoActor = context.spawn(TodoListActor(),"todoActor")

  val regexToActorMap = {
    HelloCommandActor.commandRegexes.map(_ -> helloActor) ++
      WeatherCommandActor.commandRegexes.map(_ -> weatherActor) ++
      TodoListActor.commandRegexes.map(_ ->  todoActor) ++
      GoogleCommandActor.commandRegexes.map(_ -> googleActor)
    }.toMap

    val worker = context.spawn(BrainMinion(context.self,regexToActorMap),"Worker")

    context.watch(worker)

    val showOutput = show(context.self)

    def processCommand(output : ActorRef[MouthMessages]) : Behavior[BrainMessage] = {
      Behaviors.receiveMessage[BrainMessage]{
        case EmptyCommand =>
          output ! showOutput("You did not enter a command")
          Behaviors.same
        case GetAllCommands =>
          val commands = s"commands I can process are: \n${workers.map(formatCommand).mkString("\n\n")}"
          output ! showOutput(commands)
          Behaviors.same

        case Quit =>
          input ! Input.Quit
          Behaviors.stopped

        case Done =>
          //tell app to accept another input
          input ! Input.Next
          Behaviors.same

        case DelayedResult =>
          // for commands that result in a future
          output ! MouthActor.ShowDelayed
          Behaviors.same

        case ProcessCommand(cmd) =>
          cmd match {
            case "" =>
              context.self ! Brain.EmptyCommand
            case "commands" =>
              context.self ! Brain.GetAllCommands
            case "quit" =>
              output ! MouthActor.Quit(context.self)
            case _ =>
              worker ! BrainMinion.HandleCommand(cmd)
          }

          Behaviors.same

        case InvalidCommand(command) =>

          output ! showOutput(s"Cannot Process ${command}")
          Behaviors.same

        case ErrorProcessingCommand(errorMsg) =>
          output ! showOutput(s"ERROR : ${errorMsg}")
          Behaviors.same

        case CommandProcessed(result) =>
          output ! showOutput(result)
          Behaviors.same
      }

    }.receiveSignal{
      case (_, Terminated(ref)) =>
        context.log.error(s"${ref.path.name} stopped")
        //stop app
        Behaviors.stopped
    }

    processCommand(output)
  }

  private def show(replyTo : ActorRef[BrainMessage]) :  String => MouthActor.Show  = { msg =>
    Show(msg, replyTo)
  }
}