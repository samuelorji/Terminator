package com.example.terminator.commands

import java.io.{File, PrintWriter}

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.example.terminator.config.AppConfig
import com.example.terminator.processor.BrainMinion
import com.example.terminator.processor.BrainMinion.MinionMessage
import commands.{CommandProcessAnalytics, CommandWorker, CommandsMessage, HandleCommand}
import org.slf4j.Logger
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.Try

private [commands] trait  WeatherCommandActor extends CommandWorker with CommandProcessAnalytics  {
  override def commandRegexes: List[String] = List(
    "^weather.*"
  )

  //(fetches weather information for either Copenhagen or coordinates supplied)
  override def commandTitle: String = "weather"

  override def commands: List[String] = {
    List(
      "weather",
      "weather <latitude> <longitude>"
    )
  }

  //to make code more testable
  protected def makeHttpRequest(url : String)(implicit system: ActorSystem) : Future[String] = {
    import system.dispatcher
    val request = HttpRequest(HttpMethods.GET , Uri(url))
    for  {
      response <-  Http().singleRequest(request)
      body     <- Unmarshal(response).to[String]
    }yield {
      body
    }
  }
  def actor() : Behavior[CommandsMessage] = Behaviors.setup { (context) =>


    implicit val system =  context.system.toClassic.sys
    val logger = context.log
    setLoggerAndEc(logger,system.dispatcher)

    Behaviors.receiveMessage {
      case HandleCommand(_cmd, worker) =>
        processCommandAsync(_cmd) { cmd =>
          worker ! BrainMinion.DelayedResponse

          cmd.trim.split(" ") match {
            case Array("weather", lat, lon) =>
              val latOpt = Try(lat.trim.toDouble).toOption
              val lonOpt = Try(lon.trim.toDouble).toOption
              (latOpt, lonOpt) match {
                case (Some(latD), Some(lonD)) =>
                  if (latD <= 90 && latD >= -90 && lonD <= 180 && lonD >= -180) {
                    fetchWeatherInfo(worker, latD, lonD, context.log)
                  } else {
                   Future.successful( worker ! BrainMinion.CommandNotHandled(s"latitude $latD and longitude $lonD outside range"))
                  }

                case _ =>
                  Future.successful(worker ! BrainMinion.CommandNotHandled(s"Cannot understand command '$cmd'"))
              }
            case Array("weather") => //user wants to use default coordinates
              fetchWeatherInfo(worker, AppConfig.weatherLat, AppConfig.weatherLon, logger)

            case _ =>  Future.successful(worker ! BrainMinion.CommandNotHandled(s"Cannot understand command '$cmd'"))
          }
        }
        Behaviors.same
    }
  }

  private def fetchWeatherInfo(replyTo : ActorRef[MinionMessage], lat : Double, lon : Double, logger : Logger)(implicit system : ActorSystem)  = {
    import system.dispatcher
    val url = s"https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${AppConfig.apiKey}"
    makeHttpRequest(url).map{response =>
      val json = Json.parse(response)
      val weatherDescription = Try {((json \ "weather" ).as[JsArray].head \ "description").as[String]}.toOption
      val temperature = (json \ "main" \ "temp").as[Double]
      val location = Try { (json \ "sys" \ "country").as[String] }.toOption
      val result = s"temperature : ${(temperature - 273.15).formatted("%.2f")}\u00B0C ${weatherDescription.map(x => s"\nDescription : $x" ).getOrElse("")}" +
        s"${location.map(x => s"\nLocation : $x").getOrElse("")}"

      val formatted = s"${"-" * 25}\n Weather information:\n${"-" * 25}\n$result"

      replyTo ! BrainMinion.CommandHandled(formatted)
    }.recover{
      case ex =>
        logger.error(s"Error fetching Weather Data ${ex.getMessage}")
        replyTo ! BrainMinion.CommandNotHandled("Failed to get Weather data")
    }
  }
}

object WeatherCommandActor extends WeatherCommandActor