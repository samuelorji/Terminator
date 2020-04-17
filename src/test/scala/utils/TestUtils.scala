package utils

import scala.io.BufferedSource
import scala.util.Try

object TestUtils {
  def loadResource(file : String) : Option[String] = {
    var source  : Option[BufferedSource] = None
    try {
      source =  Some(scala.io.Source.fromResource(file))
      source.map(_.mkString)
    }catch {
      case _: Throwable => None
    }

  }

}
