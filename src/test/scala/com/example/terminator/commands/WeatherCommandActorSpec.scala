package com.example.terminator.commands

import akka.actor.ActorSystem
import com.example.terminator.TestService
import com.example.terminator.processor.BrainMinion
import utils.TestUtils

import scala.concurrent.Future
import scala.concurrent.duration._

class WeatherCommandActorSpec extends TestService {

  val successWeatherActor = testKit.spawn(new WeatherCommandActor {
    override protected def makeHttpRequest(url: String)(implicit system: ActorSystem): Future[String] = Future.successful{
      TestUtils.loadResource("weather.json").getOrElse("")
    }
  }.actor(), "successWeather")
  val failureWeatherActor = testKit.spawn(new WeatherCommandActor {
    override protected def makeHttpRequest(url: String)(implicit system: ActorSystem): Future[String] =
      Future.failed(new Exception("Failed to fetch weather data for some detail"))
  }.actor(), "failedWeather")


  "The weather actor " must {
    "reject weather commands with invalid longitude and latitude " in {
      val command = "weather 23a 54q"
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      successWeatherActor ! commands.HandleCommand(command,workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.DelayedResponse)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"Cannot understand command '$command'"))
      workerProbe.expectNoMessage(100 millis)
    }

    "reject weather commands with longitude and latitude out of range " in {
      val (latD,lonD) = ("230","540")
      val command = s"weather $latD $lonD"

      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      successWeatherActor ! commands.HandleCommand(command,workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.DelayedResponse)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"latitude ${latD.toDouble} and longitude ${lonD.toDouble} outside range"))
      workerProbe.expectNoMessage(100 millis)
    }
    "reject weather commands more than 2 arguments  " in {
      val command = "weather 23.0 54.4 45"
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      successWeatherActor ! commands.HandleCommand(command,workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.DelayedResponse)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"Cannot understand command '$command'"))
      workerProbe.expectNoMessage(100 millis)
    }

    "accept weather command with command handled " in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      successWeatherActor ! commands.HandleCommand("weather",workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.DelayedResponse)
      workerProbe.expectMessageType[BrainMinion.CommandHandled]
      workerProbe.expectNoMessage(100 millis)
    }
    "accept weather command with latitude and longitude" in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      successWeatherActor ! commands.HandleCommand("weather 50 50",workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.DelayedResponse)
      workerProbe.expectMessageType[BrainMinion.CommandHandled]
      workerProbe.expectNoMessage(100 millis)
    }
    "report failure to report weather command e" in {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      failureWeatherActor ! commands.HandleCommand("weather 50 50",workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.DelayedResponse)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled("Failed to get Weather data"))
      workerProbe.expectNoMessage(100 millis)
    }

  }
}
