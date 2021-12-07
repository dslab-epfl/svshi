package ch.epfl.core.models.application

sealed trait PermissionLevel
object PermissionLevel {
  def fromString(s: String): Option[PermissionLevel] = s.toLowerCase match {
    case _ if s == Privileged.toString => Some(Privileged)
    case _ if s == NotPrivileged.toString => Some(NotPrivileged)
    case _ => None
  }
}

case object Privileged extends PermissionLevel {
  override def toString: String = "privileged"
}
case object NotPrivileged extends PermissionLevel {
  override def toString: String = "notPrivileged"
}
