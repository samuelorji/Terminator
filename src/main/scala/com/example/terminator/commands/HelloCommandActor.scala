package com.example.terminator.commands

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.terminator.processor.BrainMinion
import com.example.terminator.utils.Utils
import commands._


object HelloCommandActor extends CommandWorker with CommandProcessAnalytics {
  override def commandTitle: String = "hello"

  override def commands: List[String] = commandRegexes

  override def commandRegexes: List[String] = List(
    "hola",
    "yo",
    "hello",
    "hi"
  )

  def apply() : Behavior[CommandsMessage] = Behaviors.setup{context =>
    implicit val system =  context.system
    val logger = context.log
    setLoggerAndEc(logger,system.executionContext)


    Behaviors.receiveMessage {
      case HandleCommand(_cmd, worker) =>
        processCommand(_cmd) { _ =>
          worker ! BrainMinion.CommandHandled(getRandomHelloPhrase)
        }
        Behaviors.same
    }
  }


  private def getRandomHelloPhrase: String = {
    val phrases = List(
      "Good day",
      "Hello!",
      "How are you?",
      "Buenos dias",
      "It's a good day to be alive.",
      "Yo")
    Utils.getRandomElement(phrases)
  }

}
