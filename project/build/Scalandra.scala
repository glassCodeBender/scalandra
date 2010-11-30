import sbt._

class ScalandraProject(info: ProjectInfo) extends DefaultProject(info) {
  val ibiblioRepo = "iBiblio Maven 2 Repository" at "http://www.ibiblio.org/maven2"
  val commonsPool = "commons-pool" % "commons-pool" % "1.5.4"
  val slf4j = "org.slf4j" % "slf4j-simple" % "1.5.11"

  val specs = "org.scala-tools.testing" % "specs_2.8.0" % "1.6.5" % "test" withSources()
}
