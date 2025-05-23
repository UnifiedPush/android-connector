# UnifiedPush android-connector
![Release](https://jitpack.io/v/UnifiedPush/android-connector.svg)

This is a library that can be used by an end user application to receive notifications from any unified push provider.
An [example application](https://codeberg.org/UnifiedPush/android-example) is available to show basic usage of the library.

# Documentation

General documentation is available at <https://unifiedpush.org>

## Generate documentation

Documentation for this library can be generated with [Dokka](https://kotlinlang.org/docs/dokka-introduction.html):

```console
$ ./gradlew dokkaHtml
```

## Generate documentation for all UnifiedPush modules:

The steps for all UnifiedPush modules are included in [scripts/doc.sh]. It does the following:

1. Clone the repositories you wish to generate documentation
2. Checkout the version of the different repositories
3. Add the modules to the project settings
4. Run `dokkaHtmlMultiModule`

```console
$ ./gradlew dokkaHtmlMultiModule
```

# Funding

This project is funded through [NGI Zero Core](https://nlnet.nl/core), a fund established by [NLnet](https://nlnet.nl) with financial support from the European Commission's [Next Generation Internet](https://ngi.eu) program. Learn more at the [NLnet project page](https://nlnet.nl/project/UnifiedPush).

[<img src="https://codeberg.org/UnifiedPush/documentation/raw/branch/main/static/img/nlnet_banner.png" alt="NLnet foundation logo" width="20%" />](https://nlnet.nl)
[<img src="https://codeberg.org/UnifiedPush/documentation/raw/branch/main/static/img/NGI0_tag.svg" alt="NGI Zero Logo" width="20%" />](https://nlnet.nl/core)
