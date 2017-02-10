#!/bin/bash

if [[ $TRAVIS_BRANCH == "stable" ]]; then
	echo "Doing deploy"
	echo "<settings><servers><server><id>bintray</id><username>\${env.BINTRAY_USER}</username><password>\${env.BINTRAY_PASS}</password></server></servers></settings>" > ~/settings.xml
	mvn deploy --settings ~/settings.xml
else
	echo "Skip deploy not on stable"
fi
