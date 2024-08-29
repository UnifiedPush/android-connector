#!/bin/sh -e

NORMAL=$'\e[0m'
GREEN=$(tput setaf 2)
RED=$(tput setaf 1)

## Read details

ARTIFACT_ID=$( ./gradlew printArtifactId | sed -n '/^artifact=/ s/.*=//p' )
RELEASE_VERSION=$( ./gradlew printVersion | sed -n '/^version=/ s/.*=//p' )
DIR="$( echo "${ARTIFACT_ID:?}" | sed 's-[.:]-/-g' )/${RELEASE_VERSION:?}"
TOKEN_GPG_FILE="$HOME/.password-store/mavenCentral/token.gpg"

## Confirm

echo "You are going to release ${GREEN}${ARTIFACT_ID:?}${NORMAL}:${RED}${RELEASE_VERSION:?}${NORMAL}"
echo "Type ${RED}${RELEASE_VERSION}${NORMAL} to confirm:"
read CONFIRMATION
if [ "${RELEASE_VERSION:?}" != "${CONFIRMATION}" ]; then
    echo "Aborting."
    exit 1
fi
echo "Confirmed."

## Get token

if [ ! -f $TOKEN_GPG_FILE ]; then
    echo "[!] $TOKEN_GPG_FILE not found. Aborting."
    exit 1
fi
TOKEN=$(gpg --decrypt $TOKEN_GPG_FILE)

if [ $? -ne 0 ]; then
    echo "An error occured while decrypting the token. Aborting."
    exit 1
fi

## Build

rm -rf ~/.m2/repository/${DIR:?}
./gradlew publishReleasePublicationToMavenLocal

## Sign

cd ~/.m2/repository/
for file in ${DIR:?}/*; do
    echo "[+] Signing $file"
    sha1sum $file | sed 's/ .*//' > $file.sha1
    md5sum $file | sed 's/ .*//' > $file.md5
    gpg -ab $file
done

## Zip

ZIP_FILE="${ARTIFACT_ID:?}:${RELEASE_VERSION:?}.zip "

zip -r ${ZIP_FILE:?} ${DIR:?}/

## Upload

echo "Do you want to upload ${ZIP_FILE} ? Type ${RED}yes${NORMAL}"

read CONFIRMATION

if [ "$CONFIRMATION" = "yes" ]; then
    curl --request POST \
        --header "Authorization: Bearer ${TOKEN:?}" \
        --form bundle=@${ZIP_FILE:?} \
        https://central.sonatype.com/api/v1/publisher/upload
fi
