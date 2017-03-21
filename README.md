# Zync Android

This project is the open source Android client for Zync, 
the service which syncs your clipboard across your devices. 
Please note that although Zync is open source, our credentials
for the services we use are _not_ included in this repository for obvious reasons.

## Compiling

Setting up the environment for this project involves two steps:

- Setting up your `google-services.json` file
- Entering details into `keystore.properties`

### Google Services

Firebase, and resultantly, Google Play Services requires the `google-services.json` to
be in the `app` folder. You can get this file by doing the following:

- Enter into your [Firebase Console](https://console.firebase.google.com/), and go to your project. If you do not have one, create one.
You can also import a Google project by clicking _Import Google Project_
- Click _Add Firebase to Android App_ and follow the instructions there. This may have already been 
done for you if you imported a Google project, and you can download the `google-services.json` file
by following [these](https://support.google.com/firebase/answer/7015592#android) instructions
- When asked for the project's package name, enter `co.zync.zync`
- When finished, you can download the `google-services.json` file and move it to the `app` folder

These instructions were based off of [this](https://firebase.google.com/docs/android/setup#manually_add_firebase)

### Keystore

`keystore.properties` is a file which sits in the root directory of the project and contains
authentication details for things such as OAuth services. Here is the template for the file:

```#properties
FacebookApplicationId="fbapp_id"
```

Simply replace the slots for their corresponding values. For example, replace `fbapp_id` with the Application ID
given to you when you register an App on Facebook.