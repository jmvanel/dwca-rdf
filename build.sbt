
name := "Darwin Core Archive to RDF"

resolvers += "gbif-all" at "https://repository.gbif.org/content/groups/gbif/"

val gitHubId="gbif/dwca-io"

cancelable := true

developers := List( Developer( id="jmvanel", name="Jean-Marc Vanel", email="jeanmarc.vanel@gmail.com", url=new java.net.URL("http://jmvanel.free.fr/jmv.rdf#me")) )

// define the statements initially evaluated when entering 'console', 'console-quick', but not 'console-project'
initialCommands in console := """
                                |""".stripMargin

javacOptions ++= Seq(
  "-Xlint:deprecation",
  "-Xlint:unchecked",
  "-g:vars"
)

val guava_version = "23.0"
val jenaVersion =  "3.16.0"
val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion

libraryDependencies ++= Seq(
  "org.gbif" % "dwca-io" % "2.8" % "compile",
  "org.gbif" %  "dwc-api" % "1.25" % "compile",
  "org.gbif" %  "gbif-common" % "0.45" % "compile",
  "org.mockito" %  "mockito-core" % "2.8.47" % "test",
  "commons-io" %  "commons-io" % "2.6" % "compile",
  "org.apache.commons" %  "commons-lang3" % "3.9" % "compile",
  "org.apache.commons" %  "commons-digester3" % "3.2" % "compile",
  "javax.validation" %  "validation-api" % "1.1.0.Final" % "compile",
  "org.freemarker" %  "freemarker" % "2.3.28" % "compile",
  jenaDependency ,
  "org.slf4j" %  "slf4j-api" % "1.7.25" % "compile",
  "junit" %  "junit" % "4.12" % "test",
  "ch.qos.logback" %  "logback-classic" % "1.2.3" % "test" ,
  "com.google.guava" % "guava" % guava_version,

  "org.scalatest"     %% "scalatest"   % "3.1.0" % Test withSources(),
  "junit"             %  "junit"       % "4.12"  % Test
)

Global / onChangedBuildSource := ReloadOnSourceChanges

licenses += ("CC0", url("https://creativecommons.org/publicdomain/zero/1.0/"))

logBuffered in Test := false

logLevel := Level.Warn

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn

// Level.INFO is needed to see detailed output when running tests
logLevel in test := Level.Info

resolvers ++= Seq(
)

scalacOptions ++= Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
)

scalacOptions ++=
  scalaVersion {
    case sv if sv.startsWith("2.13") => List(
    )

    case sv if sv.startsWith("2.12") => List(
      "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification",             // Enable partial unification in type constructor inference
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
      "-Ywarn-numeric-widen"               // Warn when numerics are widened.
    )

    case _ => Nil
  }.value

// The REPL can’t cope with -Ywarn-unused:imports or -Xfatal-warnings so turn them off for the console
scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")

scalacOptions in (Compile, doc) ++= baseDirectory.map {
  bd: File => Seq[String](
     "-sourcepath", bd.getAbsolutePath, // todo replace my-new-project with the github project name
     "-doc-source-url", s"https://github.com/$gitHubId/my-new-project/tree/master€{FILE_PATH}.scala"
  )
}.value

//scalaVersion := "2.12.11"   // comment this line to use Scala 2.13
// scalaVersion := "2.13.3" // comment this line to use Scala 2.12
scalaVersion in ThisBuild := "2.12.12"

scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/$gitHubId/$name"),
    s"git@github.com:$gitHubId/$name.git"
  )
)

version := "0.1.0"

watchTriggeredMessage in ThisBuild := Watch.clearScreenOnTrigger

