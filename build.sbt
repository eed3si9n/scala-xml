import sbtcrossproject.CrossType
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import ScalaModulePlugin._

crossScalaVersions in ThisBuild := List("2.12.8", "2.13.0")

lazy val configSettings: Seq[Setting[_]] = Seq(
  unmanagedSourceDirectories ++= {
    unmanagedSourceDirectories.value.flatMap { dir =>
      val sv = scalaVersion.value
      Seq(
        CrossVersion.partialVersion(sv) match {
          case Some((2, 13)) => file(dir.getPath ++ "-2.13+")
          case _             => file(dir.getPath ++ "-2.13-")
        },
        CrossVersion.partialVersion(sv) match {
          case Some((2, _))  => file(dir.getPath ++ "-2.x")
          case _             => file(dir.getPath ++ "-3.x")
        }
      )
    }
  }
)


lazy val xml = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(scalaModuleSettings)
  .jvmSettings(scalaModuleSettingsJVM)
  .jvmSettings(
    crossScalaVersions += "0.16.0-RC3"
  )
  .settings(
    name    := "scala-xml",
    version := "2.0.0-SNAPSHOT",

    scalacOptions ++= {
      val opts =
        if (isDotty.value)
          "-language:Scala2"
        else
          // Compiler team advised avoiding the -Xsource:2.14 option for releases.
          // The output with -Xsource should be periodically checked, though.
          "-deprecation:false -feature -Xlint:-stars-align,-nullary-unit,_"
      opts.split("\\s+").to[Seq]
    },

    scalacOptions in Test  += "-Xxml:coalescing",

    mimaPreviousVersion := {
      if (System.getenv("SCALAJS_VERSION") == "1.0.0-M8") None // No such release yet
      else Some("1.2.0")
    },
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      Seq(
        // scala-xml 1.1.1 deprecated XMLEventReader, so it broke
        // binary compatibility for 2.0.0 in the following way:
        exclude[MissingClassProblem]("scala.xml.pull.EvComment"),
        exclude[MissingClassProblem]("scala.xml.pull.EvComment$"),
        exclude[MissingClassProblem]("scala.xml.pull.EvElemEnd"),
        exclude[MissingClassProblem]("scala.xml.pull.EvElemEnd$"),
        exclude[MissingClassProblem]("scala.xml.pull.EvElemStart"),
        exclude[MissingClassProblem]("scala.xml.pull.EvElemStart$"),
        exclude[MissingClassProblem]("scala.xml.pull.EvEntityRef"),
        exclude[MissingClassProblem]("scala.xml.pull.EvEntityRef$"),
        exclude[MissingClassProblem]("scala.xml.pull.EvProcInstr"),
        exclude[MissingClassProblem]("scala.xml.pull.EvProcInstr$"),
        exclude[MissingClassProblem]("scala.xml.pull.EvText"),
        exclude[MissingClassProblem]("scala.xml.pull.EvText$"),
        exclude[MissingClassProblem]("scala.xml.pull.ExceptionEvent"),
        exclude[MissingClassProblem]("scala.xml.pull.ExceptionEvent$"),
        exclude[MissingClassProblem]("scala.xml.pull.ProducerConsumerIterator"),
        exclude[MissingClassProblem]("scala.xml.pull.XMLEvent"),
        exclude[MissingClassProblem]("scala.xml.pull.XMLEventReader"),
        exclude[MissingClassProblem]("scala.xml.pull.XMLEventReader$POISON$"),
        exclude[MissingClassProblem]("scala.xml.pull.XMLEventReader$Parser"),
        exclude[MissingClassProblem]("scala.xml.pull.package"),
        exclude[MissingClassProblem]("scala.xml.pull.package$"),
        exclude[MissingTypesProblem]("scala.xml.Atom"),
        exclude[MissingTypesProblem]("scala.xml.Comment"),
        exclude[MissingTypesProblem]("scala.xml.Document"),
        exclude[MissingTypesProblem]("scala.xml.EntityRef"),
        exclude[MissingTypesProblem]("scala.xml.PCData"),
        exclude[MissingTypesProblem]("scala.xml.ProcInstr"),
        exclude[MissingTypesProblem]("scala.xml.SpecialNode"),
        exclude[MissingTypesProblem]("scala.xml.Text"),
        exclude[MissingTypesProblem]("scala.xml.Unparsed"),
        exclude[DirectMissingMethodProblem]("scala.xml.Elem.this"),
        exclude[DirectMissingMethodProblem]("scala.xml.Elem.apply"),
        exclude[DirectMissingMethodProblem]("scala.xml.Elem.processXml"),
        exclude[DirectMissingMethodProblem]("scala.xml.Elem.xmlToProcess"),
        // Scala 2.12 deprecated mutable.Stack, so we broke
        // binary compatibility for 2.0.0 in the following way:
        exclude[IncompatibleMethTypeProblem]("scala.xml.parsing.FactoryAdapter.scopeStack_="),
        exclude[IncompatibleResultTypeProblem]("scala.xml.parsing.FactoryAdapter.hStack"),
        exclude[IncompatibleResultTypeProblem]("scala.xml.parsing.FactoryAdapter.scopeStack"),
        exclude[IncompatibleResultTypeProblem]("scala.xml.parsing.FactoryAdapter.attribStack"),
        exclude[IncompatibleResultTypeProblem]("scala.xml.parsing.FactoryAdapter.tagStack")
      )
    },

    apiMappings ++= Map(
      scalaInstance.value.libraryJar
        -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")
    ) ++ {
      // http://stackoverflow.com/questions/16934488
      Option(System.getProperty("sun.boot.class.path")).flatMap { classPath =>
        classPath.split(java.io.File.pathSeparator).find(_.endsWith(java.io.File.separator + "rt.jar"))
      }.map { jarPath =>
        Map(
          file(jarPath)
            -> url("http://docs.oracle.com/javase/8/docs/api")
        )
      } getOrElse {
        // If everything fails, jam in Java 11 modules.
        Map(
          file("/modules/java.base")
            -> url("https://docs.oracle.com/en/java/javase/11/docs/api/java.base"),
          file("/modules/java.xml")
            -> url("https://docs.oracle.com/en/java/javase/11/docs/api/java.xml")
        )
      }
    }
  )
  .settings(
    inConfig(Compile)(configSettings) ++ inConfig(Test)(configSettings)
  )
  .jvmSettings(
    OsgiKeys.exportPackage := Seq(s"scala.xml.*;version=${version.value}"),

    libraryDependencies += "junit" % "junit" % "4.12" % Test,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.9" % Test,
    libraryDependencies ++= {
      if (isDotty.value)
        Seq()
      else
        Seq(("org.scala-lang" % "scala-compiler" % scalaVersion.value % Test).exclude("org.scala-lang.modules", s"scala-xml_${scalaBinaryVersion.value}"))
    }
  )
  .jsSettings(
    // Scala.js cannot run forked tests
    fork in Test := false
  )
  .jsConfigure(_.enablePlugins(ScalaJSJUnitPlugin))

lazy val xmlJVM = xml.jvm
lazy val xmlJS = xml.js
