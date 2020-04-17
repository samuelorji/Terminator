package com.example.terminator.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.example.terminator.processor.Brain.InvalidCommand
import commands.CommandsMessage

object BrainMinion {

  sealed trait MinionMessage

  case class HandleCommand(command : String) extends MinionMessage
  case class CommandHandled(result : String) extends MinionMessage
  case class CommandNotHandled(why : String) extends MinionMessage
  case object DelayedResponse extends MinionMessage

  def apply(brain: ActorRef[Brain.BrainMessage], phraseMap: Map[String, ActorRef[CommandsMessage]]): Behavior[MinionMessage] =

    Behaviors.setup { context =>
      Behaviors.receiveMessage {

        case HandleCommand(cmd) =>
          phraseMap.find { case (regex, _) => cmd.matches(regex) } match {
            case Some((_, worker)) =>
              worker ! commands.HandleCommand(cmd, context.self)
            case None =>
              brain ! InvalidCommand(cmd)
          }
          Behaviors.same

        case CommandNotHandled(error) =>
          brain ! Brain.ErrorProcessingCommand(error)
          Behaviors.same

        case DelayedResponse =>
          brain ! Brain.DelayedResult
          Behaviors.same

        case CommandHandled(result) =>
          brain ! Brain.CommandProcessed(result)
          Behaviors.same
      }

    }
}
