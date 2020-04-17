package com.example.terminator.commands

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import com.example.terminator.TestService
import com.example.terminator.processor.BrainMinion

class TodoListActorSpec extends TestService {

  val todoActor = testKit.spawn(TodoListActor(),"google")

  "The todo list Actor " must {
    "return a 'No Todo' string when there is no todo " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command = "todo list"
      todoActor ! commands.HandleCommand(command,workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandHandled("No Todo"))
    }
    "return a todo list empty string when removing from an empty list " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command = "todo rm 5"
      todoActor ! commands.HandleCommand(command,workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled("Todo list is empty"))
    }

    "correctly add a todo " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command1 = "todo add clean your room0"
      val command2 = "todo add clean your room1"
      val command3 = "todo add clean your room2"

      /*** Add three TODOs */
      todoActor ! commands.HandleCommand(command1,workerProbe.ref)
      todoActor ! commands.HandleCommand(command2,workerProbe.ref)
      todoActor ! commands.HandleCommand(command3,workerProbe.ref)
      val msg = workerProbe.receiveMessages(3)

      //all messages were successfully added
      assert(msg.collect { case e : BrainMinion.CommandHandled => e }.length == 3)

      //simulate asking for all elements
      todoActor ! commands.HandleCommand("todo list",workerProbe.ref)

      val list = workerProbe.expectMessageType[BrainMinion.CommandHandled].result
      //list should contain all the tasks
      assert(list.contains("room0") && list.contains("room1") && list.contains("room2"))
    }

    "correctly remove a todo " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command1 = "todo add clean your room0"
      val command2 = "todo add clean your room1"
      val command3 = "todo add clean your room2"
      todoActor ! commands.HandleCommand(command1,workerProbe.ref)
      todoActor ! commands.HandleCommand(command2,workerProbe.ref)
      todoActor ! commands.HandleCommand(command3,workerProbe.ref)
      val msg = workerProbe.receiveMessages(3)

      //all messages were successfully added
      assert(msg.collect { case e : BrainMinion.CommandHandled => e }.length == 3)

      /*** Remove one TODOs with id 1*/
      todoActor ! commands.HandleCommand("todo rm 1",workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandHandled("'clean your room1' removed from todo list"))

      //simulate asking for all elements
      todoActor ! commands.HandleCommand("todo list",workerProbe.ref)

      val list = workerProbe.expectMessageType[BrainMinion.CommandHandled].result
      //list should not contain 'todos' with id 2
      assert(list.contains("room0") && !list.contains("room1") && list.contains("room2"))
    }

    "correctly list all todos " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command1 = "todo add clean your room0"
      val command2 = "todo add clean your room1"
      val command3 = "todo add clean your room2"

      /*** Add three TODOs */
      todoActor ! commands.HandleCommand(command1,workerProbe.ref)
      todoActor ! commands.HandleCommand(command2,workerProbe.ref)
      todoActor ! commands.HandleCommand(command3,workerProbe.ref)
      val msg = workerProbe.receiveMessages(3)

      //all messages were successfully added
      assert(msg.collect { case e : BrainMinion.CommandHandled => e }.length == 3)

      //simulate asking for all elements
      todoActor ! commands.HandleCommand("todo list",workerProbe.ref)

      val list = workerProbe.expectMessageType[BrainMinion.CommandHandled].result

      //should contain all the elements nee
      assert(list.contains("0:") && list.contains("1:") && list.contains("2:") )
    }

    "reject removing todos with index out of bounds " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command1 = "todo add clean your room0"
      val command2 = "todo add clean your room1"
      val command3 = "todo add clean your room2"

      /*** Add three TODOs */
      todoActor ! commands.HandleCommand(command1,workerProbe.ref)
      todoActor ! commands.HandleCommand(command2,workerProbe.ref)
      todoActor ! commands.HandleCommand(command3,workerProbe.ref)
      val msg = workerProbe.receiveMessages(3)

      //all messages were successfully added
      assert(msg.collect { case e : BrainMinion.CommandHandled => e }.length == 3)

      //simulate asking for a todos id that don't exist
      val badId = 34
      todoActor ! commands.HandleCommand(s"todo rm $badId",workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"No todo found for index $badId"))
    }

    "reject removing todos with worng arguments " in  {
      val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")
      clearTodo(todoActor,workerProbe)
      val command = "todo add clean your room0" //successful
      val command1 = "todo addy clean your room0" //addy instead of add
      val command2 = "todo rm 5.2" //remove with a non integer
      val command3 = "todo listy" //listy instead of list

      /*** Add three TODOs */
      todoActor ! commands.HandleCommand(command,workerProbe.ref)
      todoActor ! commands.HandleCommand(command1,workerProbe.ref)
      todoActor ! commands.HandleCommand(command2,workerProbe.ref)
      todoActor ! commands.HandleCommand(command3,workerProbe.ref)
      val msg = workerProbe.receiveMessages(4)

      //3 messages resulted in a not handled
      assert(msg.collect { case e : BrainMinion.CommandNotHandled => e }.length == 3)

      //simulate asking for a todos id that don't exist
      val badId = 34
      todoActor ! commands.HandleCommand(s"todo rm $badId",workerProbe.ref)
      workerProbe.expectMessage(BrainMinion.CommandNotHandled(s"No todo found for index $badId"))
    }
  }

  private def clearTodo(todoActor : ActorRef[commands.CommandsMessage], worker : TestProbe[BrainMinion.MinionMessage]) : Unit = {
    todoActor ! commands.HandleCommand("todo clear",worker.ref)
    worker.receiveMessages(1)
  }
}
