package com.example.terminator.output

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.example.terminator.processor.Brain
import com.example.terminator.processor.Brain.BrainMessage

object MouthActor {

  sealed trait MouthMessages
  case class Show(msg : String,replyTo : ActorRef[BrainMessage]) extends MouthMessages
  case object ShowDelayed extends MouthMessages
  case class Quit(replyTo : ActorRef[BrainMessage]) extends MouthMessages



  def apply() : Behavior[MouthMessages] = Behaviors.receive{ (context, msg) =>
    msg match {

      case ShowDelayed =>
        println("Please wait ... ")
        Behaviors.same

      case Show(msg,replyTo) =>
        println(msg)
        replyTo ! Brain.Done
        Behaviors.same

      case Quit(replyTo) =>
        println("Goodbye ... ")
        replyTo ! Brain.Quit
        Behaviors.same
    }
  }
}
