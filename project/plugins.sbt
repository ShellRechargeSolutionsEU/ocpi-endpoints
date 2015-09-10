// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

resolvers += "TNM" at "http://nexus.thenewmotion.com/content/groups/public"
addSbtPlugin("com.thenewmotion" % "sbt-build-seed" % "1.1.0" )

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")