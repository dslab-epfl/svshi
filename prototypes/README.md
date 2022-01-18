# SVSHI - prototypes

These are the applications we developed to demonstrate what SVSHI can achieve (see the white paper for more information).

They can be installed and run on a KNX installation but they require small modifications to run.

## Ventilation

This application uses a library to connect to a Google Calendar. This library uses OAuth. This means that you have to connect in a browser at some point. So you need to update the email address in the app and to run the function once locally to connect throught the browser and let the library generate the `token.pickle`.

## Plants and Door_lock

The slack token must be modified to be a valid one for a given workspace. Follow documentation about the slack API to know how to setup one for your workspace: https://api.slack.com/tutorials/tracks/getting-a-token 