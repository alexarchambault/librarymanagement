addSbtPlugin("org.scala-sbt"  % "sbt-houserules"  % "0.3.8")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt"    % "1.15")
addSbtPlugin("org.scala-sbt"  % "sbt-contraband"  % "0.4.1")
addSbtPlugin(("com.lightbend"  % "sbt-whitesource" % "0.1.9").exclude("org.boris", "pecoff4j"))

scalacOptions += "-language:postfixOps"

addSbtCoursier
