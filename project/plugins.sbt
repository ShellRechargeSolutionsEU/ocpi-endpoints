resolvers ++= Seq(
  "TNM public" at "http://nexus.thenewmotion.com/content/groups/public",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/")

// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.1")

addSbtPlugin("com.thenewmotion" % "sbt-build-seed" % "1.1.0" )

addSbtPlugin("com.github.fedragon" % "sbt-todolist" % "0.6")