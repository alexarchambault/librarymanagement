addSbtPlugin("org.scala-sbt" % "sbt-houserules"  % "0.3.5")
addSbtPlugin("org.scala-sbt" % "sbt-contraband"  % "0.3.2")
addSbtPlugin("com.lightbend" % "sbt-whitesource" % "0.1.9")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.17")

scalacOptions += "-language:postfixOps"
