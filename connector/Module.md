# Module connector

Core library to subscribe and receive push notifications with UnifiedPush.

To receive notifications with UnifiedPush, users must have a dedicated application, a _distributor_, installed on their system.

This library requires Android 4.1 or higher.

## Import the library

Add the dependency to the _module_ build.gradle. Replace {VERSION} with the [latest version](https://central.sonatype.com/artifact/org.unifiedpush.android/connector).

```groovy
dependencies {
    // ...
    implementation 'org.unifiedpush.android:connector:{VERSION}'
}
```

## Expose a receiver

<!-- Note: This must be mirrored in MessagingReceiver comments -->

You need to expose a receiver that extend [`MessagingReceiver`][org.unifiedpush.android.connector.MessagingReceiver] and override the following methods:
- [onMessage][org.unifiedpush.android.connector.MessagingReceiver.onMessage]
- [onNewEndpoint][org.unifiedpush.android.connector.MessagingReceiver.onNewEndpoint]
- [onUnregistered][org.unifiedpush.android.connector.MessagingReceiver.onUnregistered]
- [onRegistrationFailed][org.unifiedpush.android.connector.MessagingReceiver.onRegistrationFailed]

<div class="tabs">
<input class="tabs_control hidden" type="radio" id="tabs-0-receiver-0" name="tabs-0" checked>
<label class="tabs_label" for="tabs-0-receiver-0">Kotlin</label>
<div class="tabs_content">
<!-- CONTENT KOTLIN -->

```kotlin
class CustomReceiver: MessagingReceiver() {
    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        // TODO: handle message, eg. to sync remote data or show a notification to the user
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        // TODO: send new endpoint to the app server
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        // TODO: retry depending on the reason
    }

    override fun onUnregistered(context: Context, instance: String){
        // TODO: ask to register to another distributor
    }
}
```

<!-- END KOTLIN -->
</div>
<input class="tabs_control hidden" type="radio" id="tabs-0-receiver-1" name="tabs-0">
<label class="tabs_label" for="tabs-0-receiver-1">Java</label>
<div class="tabs_content">
<!-- CONTENT JAVA -->

```java
class CustomReceiver extends MessagingReceiver {
    public CustomReceiver() {
        super();
    }

    @Override
    public void onMessage(@NotNull Context context, @NotNull byte[] message, @NotNull String instance) {
        // TODO: handle message, eg. to sync remote data or show a notification to the user
    }

    @Override
    public void onNewEndpoint(@NotNull Context context, @NotNull String endpoint, @NotNull String instance) {
        // TODO: send new endpoint to the app server
    }

    @Override
    public void onRegistrationFailed(@NotNull Context context, @NotNull FailedReason reason, @NotNull String instance) {
        // TODO: retry depending on the reason
    }

    @Override
    public void onUnregistered(@NotNull Context context, @NotNull String instance) {
        // TODO: ask to register to another distributor
    }
}
```

<!-- END JAVA -->
</div>
</div>

## Edit your manifest

<!-- Note: This must be mirrored in MessagingReceiver comments -->

The receiver has to be exposed in the `AndroidManifest.xml` in order to receive the UnifiedPush messages.

```xml
      <receiver android:exported="true"  android:enabled="true"  android:name=".CustomReceiver">
          <intent-filter>
              <action android:name="org.unifiedpush.android.connector.LINKED"/>
              <action android:name="org.unifiedpush.android.connector.MESSAGE"/>
              <action android:name="org.unifiedpush.android.connector.UNREGISTERED"/>
              <action android:name="org.unifiedpush.android.connector.NEW_ENDPOINT"/>
              <action android:name="org.unifiedpush.android.connector.REGISTRATION_FAILED"/>
          </intent-filter>
      </receiver>
```

## Request registrations

Interactions with the distributor are done with the [`UnifiedPush`][org.unifiedpush.android.connector.UnifiedPush] object.

<!-- Note: This must be mirrored in UnifiedPush comments -->

### Request a new registration

You first need to pick and save the distributor the user wants to use. If there is only one installed you can directly use that one, else this must be done with a user interaction.

If you want, the library `org.unifiedpush.android:connector-ui` offers a customizable dialog that request user's choice and register to this distributor.

Once the user has chosen the distributor, you have to save it with [`saveDistributor`][org.unifiedpush.android.connector.UnifiedPush.saveDistributor]. This function must be called before [`registerApp`][org.unifiedpush.android.connector.UnifiedPush.registerApp].

When the distributor is saved, you can call [`registerApp`][org.unifiedpush.android.connector.UnifiedPush.registerApp] to request a new registration. It has optional parameters, the following example uses `messageForDistributor` and `vapid`. You can use `instance` to bring multiple-registration support to your application.

[`registerApp`][org.unifiedpush.android.connector.UnifiedPush.registerApp] have to be called from time to time, for instance when the application starts, to be sure the distributor is still installed and correctly linked.

<div class="tabs">
<input class="tabs_control hidden" type="radio" id="tabs-1-receiver-0" name="tabs-1" checked>
<label class="tabs_label" for="tabs-1-receiver-0">Kotlin</label>
<div class="tabs_content">
<!-- CONTENT KOTLIN -->

```kotlin
import org.unifiedpush.android.connector.UnifiedPush
/* ... */

// Check if a distributor is already registered
UnifiedPush.getAckDistributor(context)?.let {
    // Re-register in case something broke
    UnifiedPush.registerApp(context, messageForDistributor, vapid)
    return
}
// Get a list of distributors that are available
val distributors = UnifiedPush.getDistributors(context)
// select one or ask the user which distributor to use, eg. with a dialog
val userDistrib = yourFunc(distributors)
// save the distributor
UnifiedPush.saveDistributor(context, userDistrib)
// register your app to the distributor
UnifiedPush.registerApp(context, messageForDistributor, vapid)
```

<!-- END KOTLIN -->
</div>
<input class="tabs_control hidden" type="radio" id="tabs-1-receiver-1" name="tabs-1">
<label class="tabs_label" for="tabs-1-receiver-1">Java</label>
<div class="tabs_content">
<!-- CONTENT JAVA -->

```java
import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;
import org.unifiedpush.android.connector.UnifiedPush;
/* ... */

// Check if a distributor is already registered
if (UnifiedPush.getAckDistributor(context) != null) {
    // Re-register in case something broke
    UnifiedPush.registerApp(
        context,
        INSTANCE_DEFAULT,
        messageForDistributor,
        vapid,
        true
    );
    return;
}
// Get a list of distributors that are available
List<String> distributors = UnifiedPush.getDistributors(context);
// select one or show a dialog or whatever
String userDistrib = yourFunc(distributors);
// the below line will crash the app if no distributors are available
UnifiedPush.saveDistributor(context, userDistrib);
UnifiedPush.registerApp(
    context,
    INSTANCE_DEFAULT,
    messageForDistributor,
    vapid,
    true
);
```

<!-- END JAVA -->
</div>
</div>

### Unsubscribe

To unsubscribe, simply call [`unregisterApp`][org.unifiedpush.android.connector.UnifiedPush.unregisterApp]. Set the instance you want to unsubscribed to if you used one during registration.

It removes the distributor if this is the last instance to unregister.
