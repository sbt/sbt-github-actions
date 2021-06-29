sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.codecommit" % "sbt-github-actions" % x)
  case _       => addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.12.0")
}
