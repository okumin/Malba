package jp.co.shanon.malba.client
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.FromConfig
import java.util.UUID
import jp.co.shanon.malba.worker.MalbaProtocol
import akka.actor.Props
import scala.concurrent.ExecutionContext.Implicits.global

class MalbaClient(system: ActorSystem, from: String, timeout: FiniteDuration, maxRetry: Int) {
  val router = system.actorOf( FromConfig().props(), "malbarouter1")
  implicit val askTimeout = akka.util.Timeout(timeout * (maxRetry + 3))

  def makeRequestId: String = {
    UUID.randomUUID().toString
  }

  def addTask ( taskId: String, group: Option[String], option: Map[String, String] = Map.empty[String, String], taskType: String, task: String ): Future[MalbaProtocol.Status] = {
    val addTaskRequest = MalbaProtocol.AddTaskRequest (
      id       = makeRequestId,
      taskId   = taskId,
      from     = from,
      group    = group,
      option   = option,
      taskType = taskType,
      task     = task
    )

    (system.actorOf(Props( classOf[MalbaRequestHandler], router, timeout, maxRetry )) ask addTaskRequest).map {
      case NoResponse =>
        throw new Exception("Can't get response")
      case MalbaProtocol.AddTaskResponse(id, taskId, from, group, taskType, status, _, _, _) =>
        status
    }
  }

  def addTaskWithCheck ( taskId: String, group: Option[String], option: Map[String, String] = Map.empty[String, String], taskType: String, task: String ): Future[MalbaProtocol.Status] = {
    val addTaskRequest = MalbaProtocol.AddTaskWithCheckWorkState (
      id       = makeRequestId,
      taskId   = taskId,
      from     = from,
      group    = group,
      option   = option,
      taskType = taskType,
      task     = task
    )

    (system.actorOf(Props( classOf[MalbaRequestHandler], router, timeout, maxRetry )) ask addTaskRequest).map {
      case NoResponse =>
        throw new Exception("Can't get response")
      case MalbaProtocol.AddTaskResponse(id, taskId, from, group, taskType, status, _, _, _) =>
        status
    }
  }


  def getTask(taskType: String): Future[MalbaProtocol.GetTaskResponseBase] = {
    val getTaskRequest = MalbaProtocol.GetTaskRequest (
      id       = makeRequestId,
      from     = from,
      taskType = taskType
    )
    (system.actorOf(Props( classOf[MalbaRequestHandler], router, timeout, maxRetry )) ask getTaskRequest).map {
      case NoResponse =>
        throw new Exception("Can't get response")
      case res: MalbaProtocol.GetTaskResponseBase =>
        res
    }
  }

  def getWorkerState(taskType: String): Future[MalbaProtocol.GetWorkerStateResponse] = {
    val request = MalbaProtocol.GetWorkerStateRequest(taskType)
    (system.actorOf(Props( classOf[MalbaRequestHandler], router, timeout, maxRetry )) ask request).map {
      case NoResponse =>
        throw new Exception("Can't get response")
      case res: MalbaProtocol.GetWorkerStateResponse =>
        res
    }
  }

  def addWorker( taskType: String, actor: ActorRef ): Future[MalbaProtocol.Status] = {
    val request = MalbaProtocol.AddActorRefAsWorkerRequest (
      id = makeRequestId,
      taskType = taskType,
      actor = actor
    )
    (system.actorOf(Props( classOf[MalbaRequestHandler], router, timeout, maxRetry )) ask request).map {
      case NoResponse =>
        throw new Exception("Can't get response")
      case MalbaProtocol.AddWorkerResponse ( id, taskType, actorPath, status) =>
        status
    }
  }
}
