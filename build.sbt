import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._
import Dependencies._

promptTheme := PromptTheme(
  List(
    text("[sbt] ", fg(105)),
    text(_ => "ProfunKtor", fg(15)).padRight(" λ ")
  )
)

lazy val tutorials = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "dev.profunktor",
      scalaVersion := "2.12.8",
      version := "0.1.0-SNAPSHOT"
    )
  ),
  name := "tutorials",
  scalafmtOnCompile := true,
  libraryDependencies ++= Seq(
    compilerPlugin(Libraries.kindProjector),
    compilerPlugin(Libraries.betterMonadicFor),
    compilerPlugin(Libraries.macroParadise),
    Libraries.cats,
    Libraries.catsMeowMtl,
    Libraries.catsPar,
    Libraries.catsEffect,
    Libraries.fs2,
    Libraries.http4sDsl,
    Libraries.http4sServer,
    Libraries.http4sCirce,
    Libraries.http4sClient,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeGenericExt,
    Libraries.circeParser,
    Libraries.pureConfig,
    Libraries.log4cats,
    Libraries.logback,
    Libraries.zioCore,
    Libraries.zioCats,
    Libraries.scalaTest      % Test,
    Libraries.scalaCheck     % Test,
    Libraries.catsEffectLaws % Test
  )
)
