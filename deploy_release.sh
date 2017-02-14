#!/bin/bash

echo "<settings><servers><server><id>bintray</id><username>\${env.BINTRAY_USER}</username><password>\${env.BINTRAY_PASS}</password></server></servers></settings>" > ~/settings.xml
mvn deploy --settings ~/settings.xml
