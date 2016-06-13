#!/bin/bash

#
# This script is only intended to run in the IBM DevOps Services Pipeline Environment.
#

#!/bin/bash
echo Informing Slack...
curl -X 'POST' --silent --data-binary '{"text":"A new build for the player service has started."}' $WEBHOOK > /dev/null

echo Setting up Docker...
mkdir dockercfg ; cd dockercfg
echo -e $KEY > key.pem
echo -e $CA_CERT > ca.pem
echo -e $CERT > cert.pem
cd ..

echo Building projects using gradle...
./gradlew build 
rc=$?
if [ $rc != 0 ]
then
  echo "Gradle build failed, will NOT perform Docker steps."
  exit 1
fi

echo Building and Starting Concierge Docker Image...
cd player-wlpcfg

../gradlew buildDockerImage 
../gradlew stopCurrentContainer 
../gradlew removeCurrentContainer
../gradlew startNewEtcdContainer

rm -rf dockercfg
