#!/bin/sh

BRANCH=`git rev-parse --abbrev-ref HEAD`
if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ]; then
  echo "Deploying to maven..."
  TERM=dumb ./gradlew -PbinTrayUser=${BINTRAY_USER} -PbinTrayKey=${BINTRAY_KEY} bintrayUpload || exit 1
fi
