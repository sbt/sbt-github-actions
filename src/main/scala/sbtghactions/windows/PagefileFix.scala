package sbtghactions.windows

case class PagefileFix(minSize: String, maxSize: String, diskRoot: Option[String] = Some("C:"))
