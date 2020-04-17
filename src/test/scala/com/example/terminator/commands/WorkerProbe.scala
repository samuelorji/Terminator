package com.example.terminator.commands

import com.example.terminator.TestService
import com.example.terminator.processor.BrainMinion

trait WorkerProbe { this : TestService =>
  protected val workerProbe = testKit.createTestProbe[BrainMinion.MinionMessage]("worker")

}
