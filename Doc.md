# Set up UnifiedPush on an Android application

Add [UnifiedPush](https://unifiedpush.org) support to your application. You can also use other libraries to get a customizable dialog to ask the users what distributor they would like to use, or to get a fallback to Google's FCM if available.

To receive notifications with UnifiedPush, users must have a dedicated application, a distributor, installed on their system.

## Core Library

Use the <a href="./connector">connector</a> library to subscribe and receive push notifications with UnifiedPush.

## Customizable Dialog

One of the main purpose of UnifiedPush is to let the users chose the way they receive their notifications. If many distributors are installed on the system, you will need to ask the users what they prefere to use. The <a href="./connector_ui">connector-ui</a> library offers a dialog that ask what distributor to use before registering your application.

## Embedded FCM Distributor

If the users don't have any UnifiedPush Distributor installed and they have Google Services enabled, you may want to fallback to Google's FCM.

You can embed an FCM Distributor, and if the user doesn’t have another distributor, this one will be used. These libraries basically act like UnifiedPush Distributors, but are internal to the app and pass notifications through FCM.

There are 2 libraries doing it: one using the Google Firebase library and another entirely FOSS that doesn’t:
* The <a href="./embedded_fcm_distributor">Embedded FCM Distributor</a> is the original embedded distributor. It uses the official Google library for FCM support. However, it is proprietary thus cannot be included on app-stores like F-Droid.
* The <a href="./foss_embedded_fcm_distributor">FOSS Embedded FCM Distributor</a> is a new library. It reimplements the same API that the Google library uses. Since it’s open source, it can be used on FOSS app stores like F-Droid.

## Example implementation

An [example application](https://codeberg.org/UnifiedPush/android-example) is available to show basic usage of the libraries.

