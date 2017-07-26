import Dependencies._
import Path._
import com.typesafe.tools.mima.core._, ProblemFilters._

def baseVersion = "1.0.0-SNAPSHOT"

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := scala212,
  // publishArtifact in packageDoc := false,
  resolvers += Resolver.typesafeIvyRepo("releases"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "bintray-sbt-maven-releases" at "https://dl.bintray.com/sbt/maven-releases/",
  // concurrentRestrictions in Global += Util.testExclusiveRestriction,
  testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-w", "1"),
  javacOptions in compile ++= Seq("-Xlint", "-Xlint:-serial"),
  crossScalaVersions := Seq(scala211, scala212),
  resolvers += Resolver.sonatypeRepo("public"),
  scalacOptions := {
    val old = scalacOptions.value
    scalaVersion.value match {
      case sv if sv.startsWith("2.10") => old diff List("-Xfuture", "-Ywarn-unused", "-Ywarn-unused-import")
      case sv if sv.startsWith("2.11") => old ++ List("-Ywarn-unused", "-Ywarn-unused-import")
      case _                           => old ++ List("-Ywarn-unused", "-Ywarn-unused-import", "-YdisableFlatCpCaching")
    }
  },
  publishArtifact in Compile := true,
  publishArtifact in Test := false,
  parallelExecution in Test := false
)

val mimaSettings = Def settings (
  mimaPreviousArtifacts := Set(organization.value %% moduleName.value % "1.0.0-X18")
)

lazy val lmRoot = (project in file("."))
  .aggregate(lmCore, lmIvy)
  .settings(
    inThisBuild(
      Seq(
        homepage := Some(url("https://github.com/sbt/librarymanagement")),
        description := "Library management module for sbt",
        scmInfo := Some(ScmInfo(
          url("https://github.com/sbt/librarymanagement"), "git@github.com:sbt/librarymanagement.git"
        )),
        bintrayPackage := "librarymanagement",
        // scalafmtOnCompile := true,
        // scalafmtVersion 1.0.0-RC3 has regression
        // scalafmtVersion := "0.6.8",
        git.baseVersion := baseVersion,
        version := {
          val v = version.value
          if (v contains "SNAPSHOT") git.baseVersion.value
          else v
        }
      )),
    commonSettings,
    name := "LM Root",
    publish := {},
    publishLocal := {},
    publishArtifact in Compile := false,
    publishArtifact := false,
    customCommands
  )

lazy val lmCore = (project in file("core"))
  .enablePlugins(ContrabandPlugin, JsonCodecPlugin)
  .settings(
    commonSettings,
    name := "librarymanagement-core",
    libraryDependencies ++= Seq(jsch,
                                scalaReflect.value,
                                scalaCompiler.value,
                                launcherInterface,
                                gigahorseOkhttp,
                                okhttpUrlconnection,
                                sjsonnewScalaJson.value % Optional,
                                scalaTest,
                                scalaCheck),
    libraryDependencies ++= scalaXml.value,
    resourceGenerators in Compile += Def.task(
      Util.generateVersionFile(
        version.value, resourceManaged.value, streams.value, (compile in Compile).value
      )
    ).taskValue,
    // mimaBinaryIssueFilters ++= Seq(),
    managedSourceDirectories in Compile +=
      baseDirectory.value / "src" / "main" / "contraband-scala",
    sourceManaged in (Compile, generateContrabands) := baseDirectory.value / "src" / "main" / "contraband-scala",
    contrabandFormatsForType in generateContrabands in Compile := DatatypeConfig.getFormats,
    // WORKAROUND sbt/sbt#2205 include managed sources in packageSrc
    mappings in (Compile, packageSrc) ++= {
      val srcs = (managedSources in Compile).value
      val sdirs = (managedSourceDirectories in Compile).value
      val base = baseDirectory.value
      (((srcs --- sdirs --- base) pair (relativeTo(sdirs) | relativeTo(base) | flat)) toSeq)
    },
    mimaSettings,
    mimaBinaryIssueFilters ++= Seq(
      // method urlFactory()okhttp3.OkUrlFactory in object sbt.librarymanagement.Http does not have a correspondent in current version
      // Was private[sbt] and manually checked to be unused in Zinc or sbt
      ProblemFilters.exclude[DirectMissingMethodProblem]("sbt.librarymanagement.Http.urlFactory"),
      // method open(java.net.URL)java.net.HttpURLConnection in object sbt.librarymanagement.Http does not have a correspondent in current version
      // Was private[sbt] and manually checked to be unused in Zinc or sbt
      ProblemFilters.exclude[DirectMissingMethodProblem]("sbt.librarymanagement.Http.open"),
    )
  )
  .configure(addSbtIO, addSbtUtilLogging, addSbtUtilPosition, addSbtUtilCache)

lazy val lmIvy = (project in file("ivy"))
  .enablePlugins(ContrabandPlugin, JsonCodecPlugin)
  .dependsOn(lmCore)
  .settings(
    commonSettings,
    name := "librarymanagement-ivy",
    libraryDependencies ++= Seq(ivy, scalaTest, scalaCheck),
    managedSourceDirectories in Compile +=
      baseDirectory.value / "src" / "main" / "contraband-scala",
    sourceManaged in (Compile, generateContrabands) := baseDirectory.value / "src" / "main" / "contraband-scala",
    contrabandFormatsForType in generateContrabands in Compile := DatatypeConfig.getFormats,
    scalacOptions in (Compile, console) --= Vector("-Ywarn-unused-import", "-Ywarn-unused", "-Xlint"),
    mimaSettings,
    mimaBinaryIssueFilters ++= Seq(
      // method UnknownResolver()java.lang.String in object sbt.internal.librarymanagement.SbtIvyLogger does not have a correspondent in current version
      // Was in a private object in sbt.internal and manually checked to be unused in Zinc or sbt
      ProblemFilters.exclude[DirectMissingMethodProblem]("sbt.internal.librarymanagement.SbtIvyLogger.UnknownResolver")
    )
  )

def customCommands: Seq[Setting[_]] = Seq(
  commands += Command.command("release") { state =>
    // "clean" ::
    "+compile" ::
      "+publishSigned" ::
      "reload" ::
      state
  }
)
