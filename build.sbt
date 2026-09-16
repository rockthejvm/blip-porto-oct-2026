ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "blip-porto-oct-2026",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    // day 3 M5 runs an HTTP server; sbt's own classloader tears it down on
    // shutdown unless the app gets its own JVM
    run / fork := true,
    run / connectInput := true,
    outputStrategy := Some(StdoutOutput),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.2.0" % Test,
      // day 3, milestone 5 only - a small HTTP layer with no FP-ecosystem baggage
      "com.lihaoyi" %% "cask" % "0.11.3"
    )
  )

// Attendees: run the block you are working on. Each block's suite covers its
// stretch section too, so the stretch tests simply sit red until you get there.
addCommandAlias("block0", "testOnly com.rockthecode.day2.Block0ExercisesSuite")
addCommandAlias("blockA", "testOnly com.rockthecode.day2.BlockAExercisesSuite")
addCommandAlias("blockB", "testOnly com.rockthecode.day2.BlockBExercisesSuite")
addCommandAlias("blockC", "testOnly com.rockthecode.day2.BlockCExercisesSuite")
addCommandAlias("blockD", "testOnly com.rockthecode.day2.BlockDExercisesSuite")
addCommandAlias("blockE", "testOnly com.rockthecode.day2.BlockEExercisesSuite")
addCommandAlias("blockF", "testOnly com.rockthecode.day2.BlockFExercisesSuite")
addCommandAlias("blockG", "testOnly com.rockthecode.day2.BlockGExercisesSuite")

// Day 3 candidate: the event-sourced ledger. One test per milestone.
addCommandAlias("ledger", "testOnly com.rockthecode.ledger.MyLedgerSuite")
// Trainer only (these two reference the solution package):
// regenerate every scenario file the tools own (.in for huge, .out for everything) ...
addCommandAlias("ledgerGen", ";runMain com.rockthecode.ledger.solution.tools.GenHuge src/main/resources/ledger" +
  " ;runMain com.rockthecode.ledger.solution.tools.GenGolden src/main/resources/ledger")
// ... and check every committed scenario file: OK / NOT OK with the first differing line.
addCommandAlias("ledgerVerify", "runMain com.rockthecode.ledger.solution.tools.Verify src/main/resources/ledger")

// Trainer: verify every reference solution still passes.
// (Plain `sbt test` is not the way to do this - it stops once the deliberately
// red exercise suites fail, and never reaches the solutions.)
addCommandAlias("checkSolutions", "testOnly *SolutionsSuite *AgreementSuite *ReferenceLedgerSuite")
