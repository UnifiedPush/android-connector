UnifiedPush connector library

This is a library that can be used by an end user application to receive notifications from any unified push provider.

# Install
![Release](https://jitpack.io/v/UnifiedPush/UP-lib.svg)

Artifacts can be retrieved from the jitpack repository.

Add the jitpack repo to the **project level** build.gradle:
```
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to the **app** build.gradle. Replace {VERSION} with the release you wish to use
```
dependencies {
    // ...
    implementation 'com.github.UnifiedPush:UP-lib:{VERSION}'
}
```

# For FCM to work
Add `classpath 'com.google.gms:google-services:4.3.4'  // Google Services plugin` to you project level build.gradle
Furthermore you need to add `id 'com.google.gms.google-services'  // Google Services plugin` to your
app level build.gradle. Lastly, since this version of the lib uses play services behind the scenes
you need to add the google-services.json file from firebase to your app directory.
