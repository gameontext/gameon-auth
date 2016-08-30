#!/bin/bash

#
# This script is only intended to run in the IBM DevOps Services Pipeline Environment.
#

#!/bin/bash
echo Informing Slack...
curl -X 'POST' --silent --data-binary '{"text":"A new build for the auth service has started."}' $SLACK_WEBHOOK_PATH > /dev/null

echo Setting up Docker...
mkdir dockercfg ; cd dockercfg
echo -e $KEY > key.pem
echo -e $CA_CERT > ca.pem
echo -e $CERT > cert.pem
echo Key `echo $KEY | md5sum`
echo Ca Cert `echo $CA_CERT | md5sum`
echo Cert `echo $CERT | md5sum`
cd ..

echo Obtaining docker.
wget http://security.ubuntu.com/ubuntu/pool/main/a/apparmor/libapparmor1_2.8.95~2430-0ubuntu5.3_amd64.deb -O libapparmor.deb
sudo dpkg -i libapparmor.deb
rm libapparmor.deb
wget https://get.docker.com/builds/Linux/x86_64/docker-1.9.1 --quiet -O docker
chmod +x docker

echo Building projects using gradle...
./gradlew build
if [ $? != 0 ]
then
  echo "Gradle build failed, will NOT perform Docker steps."
  curl -X 'POST' --silent --data-binary '{"text":"Build for the auth service has failed."}' $SLACK_WEBHOOK_PATH > /dev/null
  exit -1
else
  cd auth-wlpcfg
  ../docker build -t gameon-auth -f Dockerfile .
  if [ $? != 0 ]
  then
    echo "Docker build failed, will NOT attempt to stop/rm/start-new-container."
    curl -X 'POST' --silent --data-binary '{"text":"Docker Build for the auth service has failed."}' $SLACK_WEBHOOK_PATH > /dev/null
    exit -2
  else
    echo Attempting to remove old containers.
    ../docker stop -t 0 gameon-auth || true
    ../docker rm gameon-auth || true
    echo Starting new container.
    ../docker run -d -p 9089:9080 -p 9449:9443 --link etcd -e LICENSE=accept -e ETCDCTL_ENDPOINT=http://etcd:4001 --name=gameon-auth gameon-auth
    if [ $? != 0 ]
    then
      echo "Docker run failed.. it's too late.. the damage is done already."
      curl -X 'POST' --silent --data-binary '{"text":"Docker Run for the auth service has failed."}' $SLACK_WEBHOOK_PATH > /dev/null
      exit -3
    else
      cd ..
      rm -rf dockercfg
    fi
  fi
fi
