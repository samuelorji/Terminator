package com.example.terminator.commands

import com.example.terminator.TestService
import com.example.terminator.processor.BrainMinion
import scala.concurrent.duration._
class GoogleCommandActorSpec extends TestService {

  val googleActor = testKit.spawn(GoogleCommandActor(),"google")
  "The Google command Actor " must {
    "return a command not handled when there is no search string " in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      googleActor ! commands.HandleCommand("google", workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled("no search query"))
      workerProbe.expectNoMessage(100 millis)
    }
    //Uncomment to see actual google tab open :)

//    "return command handled when supplied a search query" in {
//      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
//      googleActor ! commands.HandleCommand("google random search", workerProbe.ref)
//      workerProbe.expectMessage(BrainMinion.CommandHandled("New Chrome Tab opened"))
//      workerProbe.expectNoMessage(100 millis)
//
//    }
    "return a 'cannot understand command' " in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      val cmd = "goggxlxe helo there "
      googleActor ! commands.HandleCommand(cmd, workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"cannot understand command '$cmd'"))
      workerProbe.expectNoMessage(100 millis)

    }
  }

}
