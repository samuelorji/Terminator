package com.example.terminator

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import com.example.terminator.output.MouthActor
import com.example.terminator.processor.Brain

object Input {
  sealed trait AppMessages
  case class UserInput(msg : String) extends AppMessages
  case object Next  extends AppMessages
  case object Quit extends AppMessages
  def apply(): Behavior[AppMessages] = Behaviors.setup{ ctx =>
    //spawn brain , input and output

    val mouth = ctx.spawn(MouthActor(),"Mouth")
    val brain = ctx.spawn(Brain(mouth,ctx.self),"Brain")

    ctx.self ! UserInput(getInput)

    ctx.watch(mouth)
    ctx.watch(brain)

    Behaviors.receiveMessage[AppMessages]{
      case UserInput(input) =>
        brain ! Brain.ProcessCommand(input)
        Behaviors.same
      case Next =>
        ctx.self ! UserInput(getInput)
        Behaviors.same
      case Quit =>
        ctx.stop(mouth)
        ctx.stop(brain)
        ctx.system.terminate()
        Behaviors.stopped
    }.receiveSignal{
      case(_,Terminated(ref)) =>
        ctx.log.error(s"${ref.path.name} stopped ")
        //close the app ):
        Behaviors.stopped(() =>
        ctx.system.terminate()
        )
    }
  }
  private def getInput = {
    scala.io.StdIn.readLine("\nterminator: ")
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Input(),"Main")
  }
}
