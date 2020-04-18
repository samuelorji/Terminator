package com.example.terminator.commands
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.terminator.processor.BrainMinion
import commands.{CommandProcessAnalytics, CommandWorker, CommandsMessage, HandleCommand}


object OpenApplicationActor extends CommandWorker with CommandProcessAnalytics {
  override def commandRegexes: List[String] = List(
    "^open.*"
  )

  override def commands: List[String] = List(
    "open <app-name>"
  )

  override def commandTitle: String = "open"


  def apply(): Behavior[CommandsMessage] = Behaviors.setup { context =>
    implicit val system = context.system
    val logger = context.log

    setLoggerAndEc(logger, system.executionContext)

    Behaviors.receiveMessage {
      case HandleCommand(cmd, worker) =>
        processCommand(cmd) { command =>
          command.trim.split(" ")match {
            case Array("open", app) =>
              val os = System.getProperty("os.name").toLowerCase
              if (os.contains("window")){

              }
              else if(os.contains("linux")){
                // get all paths in a list
                val error = Array.ofDim[Byte](2*1024) //2MB
                val success = Array.ofDim[Byte](1*1024) //256KB
                val exec =  Runtime.getRuntime.exec(Array("/bin/bash", "-c", "echo $PATH"))

                val errorRead = exec.getErrorStream.read(error)
                val successRead = exec.getInputStream.read(success)

                if(errorRead < 0){
                  val paths = new String(success.take(successRead)).trim.split(":").toList
                  def findApp(paths : List[String]) : Unit = {
                    paths match {
                      case Nil =>
                        worker ! BrainMinion.CommandNotHandled(s"Can't find ${app}")
                      case h :: t =>
                        val searchResult = Runtime.getRuntime.exec(Array("/bin/bash", "-c", s"echo `ls -la $h | grep -i $app` | cut -d' ' -f10"))
                        val successRead = searchResult.getInputStream.read(success)
                        if(successRead > 1){
                          //we found the app
                          val appName = new String(success.take(successRead)).trim
                          val appProcess = Runtime.getRuntime.exec(Array("/bin/bash", "-c", s"$h/$appName"))
                          if(!appProcess.isAlive){
                            worker ! BrainMinion.CommandNotHandled(s"Can't open ${appName}")
                          }else{
                            worker ! BrainMinion.CommandHandled(s"$app opened ")
                          }
                        }else{
                          findApp(t)
                        }
                    }
                  }
                  findApp(paths)
                }else{
                  val errorMsg = new String(error.take(errorRead))
                  logger.error(s"error occurred while processing [$cmd], error : $errorMsg")
                  worker ! BrainMinion.CommandNotHandled(s"Can't open ${app}")
                }
              }
              else if(os.contains("mac")){
                val exec = Runtime.getRuntime.exec(Array("/bin/bash", "-c", s"echo `ls -la /Applications | grep -i ${app}` | cut -d' ' -f9 -f10 -f11 -f12 -f13 -f14"))
                val error = Array.ofDim[Byte](2*1024) //2MB
                val success = Array.ofDim[Byte](1*1024) //256KB

                val errorRead = exec.getErrorStream.read(error)
                val successRead = exec.getInputStream.read(success)

                if(errorRead > 0){
                  //error occurred in shell
                  val errorMsg = new String(error.take(errorRead))
                  logger.error(s"error occurred while processing [$cmd], error : $errorMsg")
                  worker ! BrainMinion.CommandNotHandled(s"Can't find ${app}")
                }else{
                  if(successRead > 1){
                    //we found app
                    val _appname = new String(success.take(successRead)).trim
                    val appProcess = Runtime.getRuntime.exec(Array("/bin/bash", "-c", s"open /Applications/'${_appname}'"))
                    val _errorRead = appProcess.getErrorStream.read(error)
                    if(_errorRead > 0){
                      val _errorMsg = new String(error.take(_errorRead)).trim
                      logger.error(s"error occurred while processing [$cmd], error : ${_errorMsg}")
                      worker ! BrainMinion.CommandNotHandled(s"Can't open ${_appname}")
                    }else{
                      worker ! BrainMinion.CommandHandled(s"$app opened ")
                    }
                  }
                }
              }else{
                worker ! BrainMinion.CommandNotHandled(s"Failure handling ${cmd}")
              }
          }
        }
        Behaviors.same

    }
  }
}
