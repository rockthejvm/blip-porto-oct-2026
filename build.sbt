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

addCommandAlias("block0", "testOnly com.rockthecode.day2.Block0ExercisesSuite")
addCommandAlias("blockA", "testOnly com.rockthecode.day2.BlockAExercisesSuite")
addCommandAlias("blockB", "testOnly com.rockthecode.day2.BlockBExercisesSuite")
addCommandAlias("blockC", "testOnly com.rockthecode.day2.BlockCExercisesSuite")
addCommandAlias("blockD", "testOnly com.rockthecode.day2.BlockDExercisesSuite")
addCommandAlias("blockE", "testOnly com.rockthecode.day2.BlockEExercisesSuite")
addCommandAlias("blockF", "testOnly com.rockthecode.day2.BlockFExercisesSuite")
addCommandAlias("blockG", "testOnly com.rockthecode.day2.BlockGExercisesSuite")

// Day 3 candidate: the event-sourced ledger
addCommandAlias("ledger", "testOnly com.rockthecode.ledger.MyLedgerSuite")
