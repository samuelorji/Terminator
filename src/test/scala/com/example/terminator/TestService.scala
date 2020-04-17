package com.example.terminator

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll}

trait TestService extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  protected val testKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()
}
