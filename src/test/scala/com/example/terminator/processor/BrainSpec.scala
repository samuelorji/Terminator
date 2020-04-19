package com.example.terminator.processor

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import com.example.terminator.{Input, TestService}
import com.example.terminator.output.MouthActor
import com.example.terminator.processor.Brain.{EmptyCommand, GetAllCommands, Quit}

import scala.concurrent.duration._

class BrainSpec extends TestService {

  val output = testKit.createTestProbe[MouthActor.MouthMessages]("mouth")
  val input = testKit.createTestProbe[Input.AppMessages]("input")
  val worker = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
  val brain = testKit.spawn(new BrainT {
    override protected def createBrainMinion(context: ActorContext[Brain.BrainMessage], regexToActorMap: Map[String, ActorRef[commands.CommandsMessage]]): ActorRef[BrainMinion.MinionMessage]
    = worker.ref
  }.actor(output.ref, input.ref),"brain")

  "The brain command Actor " must {
    "send a you did not enter a command to output when it receives empty command" in {
      //val brain = testKit.spawn(Brain.actor(output.ref, input.ref), "brain")
      brain ! EmptyCommand
      output.expectMessage(MouthActor.Show("You did not enter a command",brain))
      output.expectNoMessage(100 millis )
    }
    "send list of commands it can process " in {
      brain ! GetAllCommands
      val msg = output.expectMessageType[MouthActor.Show]
      assert(msg.msg.startsWith("commands I can process are:"))
      output.expectNoMessage(100 millis )
    }
    "send Done command to Input Actor to accept another user input  " in {
      brain ! Brain.Done
      input.expectMessage(Input.Next)
      input.expectNoMessage(100 millis)
    }
    "process empty command that should result in a you did not enter a command output" in {
      brain ! Brain.ProcessCommand("")
      //brain should send Empty Command to itself
      output.expectMessage(MouthActor.Show("You did not enter a command",brain))
      output.expectNoMessage(100 millis )

    }
    "process 'commands' command to list all commands" in {
      brain ! Brain.ProcessCommand("commands")
      //brain should send GetAllCommands to itself
      val msg = output.expectMessageType[MouthActor.Show]
      assert(msg.msg.startsWith("commands I can process are:"))
      output.expectNoMessage(100 millis )
    }

    "send all commands both valid and invalid to the worker " in {
      val invalidCommand = "shower tuesday" // command we can't handle
      val validCommand = "google my name"
      brain ! Brain.ProcessCommand(invalidCommand)
      brain ! Brain.ProcessCommand(validCommand)

      val msgs = worker.receiveMessages(2)
      assert(msgs.head == BrainMinion.HandleCommand(invalidCommand))
      assert(msgs.last == BrainMinion.HandleCommand(validCommand))

      worker.expectNoMessage(100 millis)
    }

    "send a Cannot process to output for invalid commands" in {
      val invalidCommand = "invalid command"
      brain ! Brain.InvalidCommand(invalidCommand)
      output.expectMessage(MouthActor.Show(s"Cannot Process ${invalidCommand}",brain))
      output.expectNoMessage(100 millis )
    }

    "send an error message for failed commands" in {
      val errorMsg = "we can't handle your command"
      brain ! Brain.ErrorProcessingCommand(errorMsg)
      output.expectMessage(MouthActor.Show(s"ERROR : ${errorMsg}",brain))
      output.expectNoMessage(100 millis )
    }

    "send completed result to output " in {
      val result = "here is you result"
      brain ! Brain.CommandProcessed(result)
      output.expectMessage(MouthActor.Show(s"${result}",brain))
      output.expectNoMessage(100 millis )
    }

    //should be last test in this block as actor is stopped
    "send quit command to Input Actor to stop processing  " in {
      //val brain = testKit.spawn(Brain.actor(output.ref, input.ref), "brain")
      brain ! Quit
      input.expectMessage(Input.Quit)
      input.expectNoMessage(100 millis)
    }
  }
}
