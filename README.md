# UP-lib

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
