package com.example.terminator.commands

import com.example.terminator.TestService
import com.example.terminator.processor.BrainMinion
import scala.concurrent.duration._

class OpenApplicationActorSpec  extends TestService {

  val openAppActor = testKit.spawn(OpenApplicationActor(),"openAppActor")
  "The open command Actor " must {
    "return a Cannot understand command when there are is more than a single argument " in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      val invalidCommand = "open random xxx"
      openAppActor ! commands.HandleCommand(invalidCommand, workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"Cannot understand $invalidCommand"))
      workerProbe.expectNoMessage(100 millis)

    }
    "return a Cannot find app when an invalid app is supplied" in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      val invalidCommand = "open randomxxxyyyyqqqqqqjjjjjjj"
      openAppActor ! commands.HandleCommand(invalidCommand, workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"Cannot find ${invalidCommand.split(" ")(1)}"))
      workerProbe.expectNoMessage(100 millis)
    }
  }

}
