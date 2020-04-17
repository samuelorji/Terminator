package com.example.terminator.config

import com.typesafe.config.ConfigFactory

sealed trait AppConfigT {
  val config = ConfigFactory.load()

  val weatherLat = config.getDouble("app.weather.lat")
  val weatherLon = config.getDouble("app.weather.lon")
  val apiKey     = config.getString("app.weather.apikey")

}

object AppConfig extends AppConfigT
