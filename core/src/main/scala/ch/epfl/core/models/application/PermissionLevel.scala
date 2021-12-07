package ch.epfl.core.models.application

sealed trait PermissionLevel

case object Privileged extends PermissionLevel
case object NonPrivileged extends PermissionLevel
