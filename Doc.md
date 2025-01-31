# Set up UnifiedPush on an Android application

Add [UnifiedPush](https://unifiedpush.org) support to your application. You can also use other libraries to get a customizable dialog to ask the users what distributor they would like to use, or to get a fallback to Google's FCM if available.

To receive notifications with UnifiedPush, users must have a dedicated application, a distributor, installed on their system.

## Core Library

Use the <a href="./connector">connector</a> library to subscribe and receive push notifications with UnifiedPush.

## Customizable Dialog

One of the main purpose of UnifiedPush is to let the users chose the way they receive their notifications. If many distributors are installed on the system, you will need to ask the users what they prefere to use. The <a href="./connector_ui">connector-ui</a> library offers a dialog that ask what distributor to use before registering your application.

## Embedded FCM Distributor

If the users don't have any UnifiedPush Distributor installed and they have Google Services enabled, you may want to fallback to Google's FCM.

You can embed an FCM Distributor, and if the user doesn’t have another distributor, this one will be used. The <a href="./embedded_fcm_distributor">embedded_fcm_distributor</a> library basically act like an UnifiedPush Distributor, but is internal to the app and passes notifications through FCM. It doesn't contain proprietary code.

## Example implementation

An [example application](https://codeberg.org/UnifiedPush/android-example) is available to show basic usage of the libraries.

