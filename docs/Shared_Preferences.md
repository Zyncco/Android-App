# Shared Preferences

Zync for Android uses the `SharedPreferences` class in Android
to store a variety of information as listed in the sections 
below.

Note: The preference name by default is `ZyncPrefs` and almost
always used with private mode. (see: `ZyncApplication#getPreferences()`)

## Settings

Settings are stored in `SharedPreferences` corresponding to
their key in the [pref_general.xml](/app/src/main/res/xml/pref_general.xml)
file. This is done by using the `PreferenceFragment#setSharedPreferencesName`
method 

## API Token & Encryption Pass

These values are stored in `zync_api_token` and `encryption_pass`. 
The token and encryption pass was decided to be stored on the device's
app space as it was deemed secure _enough_.

### App Isolation

Due to Android's built in app isolation (using SELinux) and permission
system, other apps would not be able to access the files. The
only instance where this can be circumvented is when a user roots
their device, however they do this at their own risk.
 
### Bad Device Security

Another risk becomes evident when a user doesn't have their device encrypted or
doesn't have a password on their device at all. That means if someone has
access to their device, they will be able to access all the files on it through USB.

### Consensus

Although these issues do exist, removing them requires changes which will _severely_ 
hurt the user experience. The changes would involve re-entering the encryption password every time the
application is restarted and logging in with their login method again. In the future,
an opt-in preference can be added where they can choose whether they want these values
to be stored on file or not. Keep in mind, though, that if someone was very picky on
the security of their applications, they wouldn't have rooted their device (or be very
picky on what they installed with root priv.) and would have their device encrypted.

## History

A local cache of their history is stored on the device but large files (or any non-text)
is stored in the preferences file as `zync_history` as a string set.