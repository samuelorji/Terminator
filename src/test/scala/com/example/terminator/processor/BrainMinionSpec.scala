package com.example.terminator.processor

import com.example.terminator.TestService
import com.example.terminator.commands._

class BrainMinionSpec extends TestService {

  val helloActor = testKit.createTestProbe[commands.CommandsMessage]("helloActor")
  val weatherActor = testKit.createTestProbe[commands.CommandsMessage]("weatherActor")
  val todoActor = testKit.createTestProbe[commands.CommandsMessage]("todoActor")
  val googleActor = testKit.createTestProbe[commands.CommandsMessage]("googleActor")
  val openAppActor = testKit.createTestProbe[commands.CommandsMessage]("openAppActor")
  val brain = testKit.createTestProbe[Brain.BrainMessage]("brain")

  val regexToActorMap = {
    HelloCommandActor.commandRegexes.map(_ -> helloActor.ref) ++
      WeatherCommandActor.commandRegexes.map(_ -> weatherActor.ref) ++
      TodoListActor.commandRegexes.map(_ -> todoActor.ref) ++
      GoogleCommandActor.commandRegexes.map(_ -> googleActor.ref) ++
      OpenApplicationActor.commandRegexes.map(_ -> openAppActor.ref)
    }.toMap

  val brainMinion = testKit.spawn(BrainMinion(brain.ref, regexToActorMap), "hello")

  "The Brain Minion " must {
    "send an Invalid command to brain for commands that don't match our regex" in {
      val invalidCommand = "shower tuesday"
      brainMinion ! BrainMinion.HandleCommand(invalidCommand)
      brain.expectMessage(Brain.InvalidCommand(invalidCommand))
      brain.expectNoMessage()
    }
    "send an error message to brain for unhandled command from worker" in {
      val error = "some error"
      brainMinion ! BrainMinion.CommandNotHandled(error)
      brain.expectMessage(Brain.ErrorProcessingCommand(error))
      brain.expectNoMessage()
    }

    "send processed message to brain for handled command from worker" in {
      val result = "bueno dias !!"
      brainMinion ! BrainMinion.CommandHandled(result)
      brain.expectMessage(Brain.CommandProcessed(result))
      brain.expectNoMessage()
    }

    "send Delayed response message to brain " in {
      brainMinion ! BrainMinion.DelayedResponse
      brain.expectMessage(Brain.DelayedResult)
    }

    "send appropriate messages to appropriate actor " in {
      val googleCommand = "google something"
      val openCommand = "open something"
      val helloCommand = "hello"
      val todoCommand = "todo add something"
      val weatherCommand = "weather"

      brainMinion ! BrainMinion.HandleCommand(googleCommand)
      googleActor.expectMessage(commands.HandleCommand(googleCommand,brainMinion))
      googleActor.expectNoMessage()
      brainMinion ! BrainMinion.HandleCommand(openCommand)
      openAppActor.expectMessage(commands.HandleCommand(openCommand,brainMinion))
      openAppActor.expectNoMessage()
      brainMinion ! BrainMinion.HandleCommand(helloCommand)
      helloActor.expectMessage(commands.HandleCommand(helloCommand,brainMinion))
      helloActor.expectNoMessage()
      brainMinion ! BrainMinion.HandleCommand(todoCommand)
      todoActor.expectMessage(commands.HandleCommand(todoCommand,brainMinion))
      todoActor.expectNoMessage()
      brainMinion ! BrainMinion.HandleCommand(weatherCommand)
      weatherActor.expectMessage(commands.HandleCommand(weatherCommand,brainMinion))
      weatherActor.expectNoMessage()
    }

  }

}
