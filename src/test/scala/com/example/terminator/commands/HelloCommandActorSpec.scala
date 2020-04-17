package com.example.terminator.commands

import com.example.terminator.TestService
import com.example.terminator.processor.BrainMinion

class HelloCommandActorSpec extends TestService{

  val helloActor = testKit.spawn(HelloCommandActor(), "hello")

  "The Hello Actor " must {
    "return a random greeting when it receives a greeting" in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      helloActor ! commands.HandleCommand("hi",workerProbe.ref)
      val receivedMessage = workerProbe.expectMessageType[BrainMinion.CommandHandled]
      assert(HelloCommandActor.responsePhrases.contains(receivedMessage.result))
    }
  }
}
