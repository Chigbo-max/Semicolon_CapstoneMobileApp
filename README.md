# anti_theft_tracker_agent

A new Flutter project.

## Getting Started

# Anti-Theft-Tracker Agent - Flutter App

A silent agent app that:
- Registers new devices enrolled via Android Management API
- Sends device metadata + last known location to backend
- Handles remote commands silently
- Persists after reboots

## Setup Instructions

### Android Permissions
Update `/android/app/src/main/AndroidManifest.xml` with:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>


This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.
