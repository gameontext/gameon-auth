#!/bin/bash

export CONTAINER_NAME=auth

certpath=/tmp/java-ssl/
mkdir -p ${certpath}

cp $JAVA_HOME/jre/lib/security/cacerts ${certpath}/cacerts

if [ "$ETCDCTL_ENDPOINT" != "" ]; then
  echo Setting up etcd...
  echo "** Testing etcd is accessible"
  etcdctl --debug ls
  RC=$?

  while [ $RC -ne 0 ]; do
      sleep 15

      # recheck condition
      echo "** Re-testing etcd connection"
      etcdctl --debug ls
      RC=$?
  done
  echo "etcdctl returned sucessfully, continuing"

  etcdctl get /proxy/third-party-ssl-cert > ${certpath}/cert.pem

  #export SYSTEM_ID=$(etcdctl get /global/system_id)

  export TWITTER_CONSUMER_KEY=$(etcdctl get /auth/twitter/id)
  export TWITTER_CONSUMER_SECRET=$(etcdctl get /auth/twitter/secret)
  export FACEBOOK_APP_ID=$(etcdctl get /auth/facebook/id)
  export FACEBOOK_APP_SECRET=$(etcdctl get /auth/facebook/secret)
  export GOOGLE_APP_ID=$(etcdctl get /auth/google/id)
  export GOOGLE_APP_SECRET=$(etcdctl get /auth/google/secret)
  export GITHUB_APP_ID=$(etcdctl get /auth/github/id)
  export GITHUB_APP_SECRET=$(etcdctl get /auth/github/secret)

  export FRONT_END_SUCCESS_CALLBACK=$(etcdctl get /auth/callback)
  export FRONT_END_FAIL_CALLBACK=$(etcdctl get /auth/failcallback)
  export FRONT_END_AUTH_URL=$(etcdctl get /auth/url)

  GAMEON_MODE=$(etcdctl get /global/mode)
  export GAMEON_MODE=${GAMEON_MODE:-production}
  export TARGET_PLATFORM=$(etcdctl get /global/targetPlatform)
fi

if [ -f /etc/cert/cert.pem ]; then
  cp /etc/cert/cert.pem ${certpath}/cert.pem
fi


# Container has requested we use the supplied cert for auth.
if [ -f ${certpath}/cert.pem ]; then
  echo "Building keystore/truststore from cert.pem"
  echo "-converting pem to pkcs12"
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out ${certpath}/cert.pkcs12 -in ${certpath}/cert.pem
  echo "-creating dummy key.jks"
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore ${certpath}/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  echo "-emptying key.jks"
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore ${certpath}/key.jks
  echo "-importing pkcs12 to key.jks"
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore ${certpath}/cert.pkcs12 -srcstoretype PKCS12 -destkeystore ${certpath}/key.jks -deststoretype JKS

  echo "-importing pem to truststore"
  keytool -import -v -trustcacerts -alias default -file ${certpath}/cert.pem -storepass changeit -keypass keystore -noprompt -keystore ${certpath}/cacerts
  echo "done"
fi

XTRA_ARGS=""
if [ "${GAMEON_MODE}" == "development" ]; then
  XTRA_ARGS="--spring.profiles.active=dummyauth"
fi

exec java -Djavax.net.ssl.trustStore=${certpath}/cacerts \
    -Djavax.net.ssl.trustStorePassword=changeit
    -Djava.security.egd=file:/dev/./urandom -jar /app.jar \
    --spring.profiles.active=default ${XTRA_ARGS}
