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
    override fun onMessage(context: Context, message: PushMessage, instance: String) {
        // TODO: handle message, eg. to sync remote data or show a notification to the user
    }

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
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
    public void onMessage(@NotNull Context context, @NotNull PushMessage message, @NotNull String instance) {
        // TODO: handle message, eg. to sync remote data or show a notification to the user
    }

    @Override
    public void onNewEndpoint(@NotNull Context context, @NotNull PushEndpoint endpoint, @NotNull String instance) {
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

### Use user's default distributor

Users are allowed to define a default distributor on their system, because UnifiedPush distributors
have to be able to process a deeplink.

When you set UnifiedPush for the first time on your application, you will want to use the default user's
distributor.

From time to time, like every time you starts your application, you should register your application in case the
user have uninstalled the previous distributor.
If the previous distributor is uninstalled, you can fallback to the default one again.

Therefore, you can use [tryUseCurrentOrDefaultDistributor][org.unifiedpush.android.connector.UnifiedPush.tryUseCurrentOrDefaultDistributor]
to select the saved distributor or the default one when your application starts (when your main activity is created for instance).

When the distributor is saved, you can call [`register`][org.unifiedpush.android.connector.UnifiedPush.register] to request a new registration.
It has optional parameters, the following example uses `messageForDistributor` and `vapid`.
You can use `instance` to bring multiple-registration support to your application.

_If you want, you can use the library `org.unifiedpush.android:connector-ui` instead, it displays a dialog explaining why
the OS picker is going to ask which application to pick._

<div class="tabs">
<input class="tabs_control hidden" type="radio" id="tabs-trydefault-receiver-0" name="tabs-trydefault" checked>
<label class="tabs_label" for="tabs-trydefault-receiver-0">Kotlin</label>
<div class="tabs_content">
<!-- CONTENT KOTLIN -->

```kotlin
import org.unifiedpush.android.connector.UnifiedPush
/* ... */

UnifiedPush.tryUseCurrentOrDefaultDistributor(context) { success ->
    if (success) {
        // We have a distributor
        // Register your app to the distributor
        UnifiedPush.register(context, messageForDistributor, vapid)
    }
}
```

<!-- END KOTLIN -->
</div>
<input class="tabs_control hidden" type="radio" id="tabs-trydefault-receiver-1" name="tabs-trydefault">
<label class="tabs_label" for="tabs-trydefault-receiver-1">Java</label>
<div class="tabs_content">
<!-- CONTENT JAVA -->

```java
import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;
import org.unifiedpush.android.connector.UnifiedPush;
/* ... */

UnifiedPush.tryUseCurrentOrDefaultDistributor(context, success ->{
    if (success) {
        // We have a distributor
        // Register your app to the distributor
        UnifiedPush.register(
            context,
            INSTANCE_DEFAULT,
            messageForDistributor,
            vapid
        );
    }
});
```

<!-- END JAVA -->
</div>
</div>

Be aware that [tryUseDefaultDistributor][org.unifiedpush.android.connector.UnifiedPush.tryUseDefaultDistributor]
starts a new translucent activity in order to get the result of the distributor activity. You may prefer to use
[LinkActivityHelper][org.unifiedpush.android.connector.LinkActivityHelper] directly in your own activity instead.

### Use another distributor

You will probably want to allow the users to use another distributor but their default one.

For this, you can get the list of available distributors with [`getDistributors`][org.unifiedpush.android.connector.UnifiedPush.getDistributors].

Once the user has chosen the distributor, you have to save it with [`saveDistributor`][org.unifiedpush.android.connector.UnifiedPush.saveDistributor].
This function must be called before [`register`][org.unifiedpush.android.connector.UnifiedPush.register].

When the distributor is saved, you can call [`register`][org.unifiedpush.android.connector.UnifiedPush.register] to request a new registration.
It has optional parameters, the following example uses `messageForDistributor` and `vapid`.
You can use `instance` to bring multiple-registration support to your application.

_If you want, the library `org.unifiedpush.android:connector-ui` offers a customizable dialog
that request user's choice and register to this distributor._

<div class="tabs">
<input class="tabs_control hidden" type="radio" id="tabs-1-receiver-0" name="tabs-1" checked>
<label class="tabs_label" for="tabs-1-receiver-0">Kotlin</label>
<div class="tabs_content">
<!-- CONTENT KOTLIN -->

```kotlin
import org.unifiedpush.android.connector.UnifiedPush
/* ... */

// Get a list of distributors that are available
val distributors = UnifiedPush.getDistributors(context)
// select one or ask the user which distributor to use, eg. with a dialog
val userDistrib = yourFunc(distributors)
// save the distributor
UnifiedPush.saveDistributor(context, userDistrib)
// register your app to the distributor
UnifiedPush.register(context, messageForDistributor, vapid)
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

// Get a list of distributors that are available
List<String> distributors = UnifiedPush.getDistributors(context);
// select one or show a dialog or whatever
String userDistrib = yourFunc(distributors);
// the below line will crash the app if no distributors are available
UnifiedPush.saveDistributor(context, userDistrib);
UnifiedPush.register(
    context,
    INSTANCE_DEFAULT,
    messageForDistributor,
    vapid
);
```

<!-- END JAVA -->
</div>
</div>

### Unsubscribe

To unsubscribe, simply call [`unregister`][org.unifiedpush.android.connector.UnifiedPush.unregister]. Set the instance you want to unsubscribed to if you used one during registration.

It removes the distributor if this is the last instance to unregister.
