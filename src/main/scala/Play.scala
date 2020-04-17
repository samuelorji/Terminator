import MasterControlProgram.SpawnJob
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop, Terminated}
import play.api.libs.json._

object Play {


  val json = "{\"coord\":{\"lon\":12.57,\"lat\":55.68},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"base\":\"stations\",\"main\":{\"temp\":279.96,\"feels_like\":274.16,\"temp_min\":279.15,\"temp_max\":281.15,\"pressure\":1017,\"humidity\":78},\"visibility\":10000,\"wind\":{\"speed\":6.2,\"deg\":270},\"clouds\":{\"all\":100},\"dt\":1586895281,\"sys\":{\"type\":1,\"id\":1575,\"country\":\"DK\",\"sunrise\":1586837182,\"sunset\":1586887981},\"timezone\":7200,\"id\":6949461,\"name\":\"Inner City\",\"cod\":200}"
  val json2 = "{\"coord\":{\"lon\":12.57,\"lat\":55.68},\"weather\":[],\"base\":\"stations\",\"main\":{\"temp\":279.96,\"feels_like\":274.16,\"temp_min\":279.15,\"temp_max\":281.15,\"pressure\":1017,\"humidity\":78},\"visibility\":10000,\"wind\":{\"speed\":6.2,\"deg\":270},\"clouds\":{\"all\":100},\"dt\":1586895281,\"sys\":{\"type\":1,\"id\":1575,\"country\":\"DK\",\"sunrise\":1586837182,\"sunset\":1586887981},\"timezone\":7200,\"id\":6949461,\"name\":\"Inner City\",\"cod\":200}"

  val parsed = Json.parse(json2)



  val arr = Array.ofDim[Byte](1 * 1024)

  val list = List(
    "weather",
    "weather <latitude> <longitude>"
  )

  val master = ActorSystem(MasterControlProgram(),"master")

  master ! SpawnJob("Hello")



  //println(formatted)
}

object MasterControlProgram {
  sealed trait Command
  final case class SpawnJob(name: String) extends Command

  import Job.Die
  def apply(): Behavior[Command] = {
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case SpawnJob(jobName) =>
            context.log.info("Spawning job {}!", jobName)
            val job = context.spawn(Job(jobName), name = jobName)
            println(s"job actor created $job")
            context.watch(job)

            job ! Die
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, Terminated(ref)) =>
          println(s"${ref.path.name} Stopped, restarting")
          Behaviors.same
      }
  }
}
object Job {
  sealed trait Command
  case object Die  extends Command

  def apply(name: String): Behavior[Command] = {

    Behaviors.receiveMessage[Command]{
      case Die =>
        println(s"received Die ")
        Behaviors.stopped
    }.receiveSignal{
      case (context, PostStop) =>
        println(s"Worker ${name} stopped")
        Behaviors.stopped
    }
  }
}

