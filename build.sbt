ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "ru.d10xa"

lazy val root = (project in file(".")).settings(
  name := "safeincloudxml-to-bitwardenjson",
  assemblyJarName in assembly := "safeincloudxml-to-bitwardenjson.jar",
  mainClass in assembly := Some(
    "ru.d10xa.safeincloudxml_to_bitwardenjson.Main"
  ),
  test in assembly := {},
  scalacOptions := Seq(
    "-encoding",
    "UTF-8", // source files are in UTF-8
    "-deprecation", // warn about use of deprecated APIs
    "-unchecked", // warn about unchecked type parameters
    "-feature", // warn about misused language features
    "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
    "-Xlint", // enable handy linter warnings
    "-Xfatal-warnings" // turn compiler warnings into errors
  )
)

wartremoverErrors in(Compile, compile) ++= Seq(
  Wart.Any,
  Wart.AnyVal,
  Wart.ArrayEquals,
  Wart.AsInstanceOf,
  Wart.DefaultArguments,
  Wart.EitherProjectionPartial,
  Wart.Enumeration,
  Wart.Equals,
  Wart.ExplicitImplicitTypes,
  Wart.FinalCaseClass,
  Wart.FinalVal,
  Wart.ImplicitConversion,
  Wart.ImplicitParameter,
  Wart.IsInstanceOf,
  Wart.JavaConversions,
  Wart.JavaSerializable,
  Wart.LeakingSealed,
  Wart.MutableDataStructures,
  Wart.NonUnitStatements,
  Wart.Nothing,
  Wart.Null,
  Wart.Option2Iterable,
//  Wart.OptionPartial,
  Wart.Overloading,
  Wart.Product,
  Wart.PublicInference,
  Wart.Recursion,
  Wart.Return,
  Wart.Serializable,
  Wart.StringPlusAny,
  Wart.Throw,
  Wart.ToString,
  Wart.TraversableOps,
  Wart.TryPartial,
  Wart.Var,
  Wart.While
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
libraryDependencies += "io.circe" %% "circe-core" % "0.12.3"
libraryDependencies += "com.lucidchart" %% "xtract" % "2.2.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.12.3"
libraryDependencies += "io.circe" %% "circe-parser" % "0.12.3"
libraryDependencies += "com.monovore" %% "decline" % "1.0.0"
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.8.0"
