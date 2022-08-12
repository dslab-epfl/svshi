# SVSHI Web service - Backend

This is the backend part of the web service for the SVSHI companion. 

## web-service-backend

This scala project is an HTTP server. It provides the following API to be used by the frontend. The Webservice backend serves on the port 4545.

### Relationship with svshi

The backend has a SVSHI instance running for all svshi operations. For example, it calls it for getting the device mappings, generating a new app, installing apps, ...

We do not rely on the svshi state:

- when generating an app, we call svshi, get the zip file, store it in the session of the web service and delete it from SVSHI
- to install apps, we will keep all "installed" apps in the session and everytime a user install an app, we remove everything from svshi and install all of them again.

### API

All requests with a SESSION are related to a session. This means that a session should exist for these endpoints to be used. If no session exists, a code 403 is returned.
For now, the backend supports only one session (hard coded sessionId).

- GET `/version`

  Returns the current version of `svshi-web-service`.

- GET `/sessionId`

  It creates a new session on the server if the session does not already exist. Reply with the id of the session.

- SESSION POST `/reset/session`

  Delete everything related to that session. You need to call GET `/sessionId` for your following requests to be valid again.

- SESSION POST `/addFloor/:floorName/:floorNumber`

  Add a new floor. The body must contain a `.zip` that contains the `.dxf` file of the floor plan.
  If a floor with the same number already exists, it is replaced.

- SESSION GET `/floors`

  Reply with a json array in the body containing the name of each floor.

- SESSION POST `/etsFile/`

  Set of replaces the ETS Project file for the session. The body must contain a `.zip` that contains the `.knxproj` file.
  
- SESSION GET `/floorFile/:number`

  Reply with the file for the given floor in a zip archive. 404 if the floor does not exist in the current session.

- SESSION GET `/etsFile` 

  Reply with the current ets file of the session in a zip archive. 404 if not defined

- SESSION GET `/getDeviceMappings`

  Reply with the device mappings for the ets file of the session as a json in the body. 412 code if the ets file is not yet defined. 500 if an error occured when generating the mapping.
  The json contains the mapping and the physical structure.

- SESSION POST `/generateApp/:appName`

  Using SVSHI, generate a new app using the json passed in the body and the appname from the url. The body is the prototypical structure as described in SVSHI documentation.

- SESSION GET `/applications/names`

  Reply with a json array in the body containing all applications that are uploaded in the curernt session. These are applications added through the `/applications/add/:appName` endpoint.

- SESSION GET `/applications/files/:appName`

  Reply with a zip archive containing all files for the app with given name. 404 if the app is not in the session.

- SESSION POST `/applications/add/:appName`

  Add a new application to the session. The body must contain a zip archive with all the file. The archive can either contain all files at the root, or a folder at the root with all files inside.

- SESSION POST `/applications/delete/:appName`

  Remove the app with the given name from the session. Deletes all related files.

- SESSION GET `/svshi/applications/installed/names`

  Reply with the names of the applications installed on SVSHI as json array in the body.

- SESSION POST `/svshi/applications/uninstall`
  
  Send a request to SVSHI to uninstall ALL applications.

- SESSION POST `/svshi/applications/install`

  Install the applications whose names are given in a json array in the body on SVSHI, first uninstalling all applications. The apps must be in the session.

- SESSION POST `/simulation/start`

  Start a new simulation: generate the config on the simulator and start the simulator and then start SVSHI to connect to the simulator.

- SESSION POST `/simulation/stop`

  Stop the simulator and SVSHI. Operate with best effort: send a request to both to stop and replies with a message indicating which stops or not and potential messages from the service.

- SESSION GET `/svshi/runStatus/`

  Reply with the status of SVSHI in a json object of the form `{"status": true/false}`

- SESSION GET `/simulator/runStatus/`

  Reply with the status of the simulator in a json object of the form `{"status": true/false}`

- SESSION GET `/svshi/logs/run`

  Reply with the run logs of SVSHI in a json object of the form `{"lines": ["l1", "l2"]}`

- SESSION GET `/svshi/logs/run`

  Reply with the run logs of SVSHI in a json object of the form `{"lines": ["l1", "l2"]}`

- SESSION GET `/svshi/logs/receivedTelegrams`

  Reply with the receivedTelegrams logs of SVSHI in a json object of the form `{"lines": ["l1", "l2"]}`

- SESSION GET `/svshi/logs/execution`

  Reply with the execution logs of SVSHI in a json object of the form `{"lines": ["l1", "l2"]}`
