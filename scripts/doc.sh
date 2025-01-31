#!/bin/sh

git clone --filter=blob:none https://codeberg.org/UnifiedPush/android-connector-ui
ln -s android-connector-ui/connector_ui .
git clone --filter=blob:none https://codeberg.org/UnifiedPush/android-embedded_fcm_distributor
ln -s android-embedded_fcm_distributor/embedded_fcm_distributor .

echo """
include ':connector_ui'
include ':embedded_fcm_distributor'
""" >> settings.gradle

echo "Checkout the different repo to the version you wish to generate documentation for"
echo "Y/y to continue"
read CONFIRMATION
if [ "${CONFIRMATION,,}" != "y" ]; then
    echo "Aborting."
    exit 0
fi
./gradlew dokkaHtmlMultiModule
