package ch.epfl.core

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.physical.PhysicalStructure

trait SvshiTr {
  def getVersion(success: String => Unit): Int

  def run(
      knxAddress: Option[String],
      existingAppsLibrary: ApplicationLibrary,
      blocking: Boolean
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): SvshiRunResult

  def compileApps(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      newPhysicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int

  def updateApp(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      appToUpdateName: String,
      existingPhysicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int

  def generateBindings(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      existingPhysicalStructure: PhysicalStructure,
      newPhysicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int

  def generateApp(
      appName: Option[String],
      devicesPrototypicalStructureFile: Option[String]
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int

  def removeApps(
      allFlag: Boolean,
      appName: Option[String],
      existingAppsLibrary: ApplicationLibrary
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int

  def listApps(
      existingAppsLibrary: ApplicationLibrary
  ): List[String]

  def generatePrototypicalDeviceMappings(
      physicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int

  def getAvailableProtoDevices(): List[String]
  def getAvailableDpts(): List[String]
}
