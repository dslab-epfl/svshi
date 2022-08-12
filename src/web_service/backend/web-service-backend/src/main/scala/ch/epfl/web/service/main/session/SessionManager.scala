package ch.epfl.web.service.main.session

import ch.epfl.web.service.main.utils.{Constants, FileUtils}

// Manage the sessions stored by the service
object SessionManager {
  private var currentSession: Option[Session] = None

  def getCurrentSession: Option[Session] = {
    currentSession
  }

  def createNewSession(): Session = {
    val session = new Session(getSessionId())
    currentSession = Some(session)
    session
  }

  def deleteCurrentSession(): Unit = {
    currentSession.foreach(_.delete())
    currentSession = None
  }

  def loadFromFileIfSessionExists(): Unit = {
    val possibleSessions = FileUtils.getListOfFolders(Constants.SESSIONS_FOLDER_PATH)
    if (possibleSessions.nonEmpty) {
      if (os.exists(Constants.SESSIONS_FOLDER_PATH / getSessionId())) {
        this.currentSession = Some(new Session(getSessionId()))
        this.currentSession.get.reloadFromFile()
      }
    }
  }

  private def getSessionId(): String = {
//    UUID.randomUUID.toString
    "static_session_id"
  }
}
