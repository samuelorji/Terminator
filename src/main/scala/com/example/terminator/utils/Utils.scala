package com.example.terminator.utils

import scala.util.Random

object Utils {

  val random = new Random()
  def getRandomElement[T](elements : List[T]) : T = {
    val randomIndex = random.nextInt(elements.length)
    elements(randomIndex)
  }

}
