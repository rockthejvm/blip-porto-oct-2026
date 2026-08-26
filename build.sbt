ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "blip-porto-oct-2026",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    libraryDependencies += "org.scalameta" %% "munit" % "1.2.0" % Test
  )

// Attendees: run the block you are working on. Each block's suite covers its
// stretch section too, so the stretch tests simply sit red until you get there.
addCommandAlias("block0", "testOnly com.rockthecode.day2.Block0ExercisesSuite")
addCommandAlias("blockA", "testOnly com.rockthecode.day2.BlockAExercisesSuite")

// Trainer: verify every reference solution still passes.
// (Plain `sbt test` is not the way to do this - it stops once the deliberately
// red exercise suites fail, and never reaches the solutions.)
addCommandAlias("checkSolutions", "testOnly *SolutionsSuite *AgreementSuite")
