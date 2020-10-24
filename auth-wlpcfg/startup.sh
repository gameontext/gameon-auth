#!/bin/bash

export CONTAINER_NAME=auth

src_path=/auth
# pre-created keystores should be mounted here (rather than cert.pem)
ssl_path=/auth/ssl/

if [ -f /etc/cert/cert.pem ]; then
  cp -f /etc/cert/cert.pem ${src_path}/cert.pem
fi

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

  etcdctl get /proxy/third-party-ssl-cert > ${src_path}/cert.pem

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

# Make sure keystores are present or are generated
/gen-keystore.sh ${src_path} ${ssl_path}

XTRA_ARGS=""
if [ "${GAMEON_MODE}" == "development" ]; then
  XTRA_ARGS="--spring.profiles.active=dummyauth"
fi

exec java \
  -Djavax.net.ssl.trustStore=${ssl_path}/truststore.jks \
  -Djavax.net.ssl.trustStorePassword=gameontext-trust \
  -Djava.security.egd=file:/dev/./urandom -jar /app.jar \
  --spring.profiles.active=default ${XTRA_ARGS}
