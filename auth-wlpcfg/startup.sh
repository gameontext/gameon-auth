#!/bin/bash

# Configure amalgam8 for this container
export A8_SERVICE=auth:v1
export A8_ENDPOINT_PORT=9443
export A8_ENDPOINT_TYPE=https

if [ "$SSL_CERT" != "" ]; then
  echo Found an SSL cert to use.
  cd /opt/ibm/wlp/usr/servers/defaultServer/resources/
  echo -e $SSL_CERT > cert.pem
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out cert.pkcs12 -in cert.pem
  keytool -import -v -trustcacerts -alias default -file cert.pem -storepass truststore -keypass keystore -noprompt -keystore security/truststore.jks
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore security/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore security/key.jks
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore cert.pkcs12 -srcstoretype PKCS12 -destkeystore security/key.jks -deststoretype JKS
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

  cd /opt/ibm/wlp/usr/servers/defaultServer/resources/
  etcdctl get /proxy/third-party-ssl-cert > cert.pem
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out cert.pkcs12 -in cert.pem
  keytool -import -v -trustcacerts -alias default -file cert.pem -storepass truststore -keypass keystore -noprompt -keystore security/truststore.jks
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore security/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore security/key.jks
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore cert.pkcs12 -srcstoretype PKCS12 -destkeystore security/key.jks -deststoretype JKS

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
  export LOGSTASH_ENDPOINT=$(etcdctl get /logstash/endpoint)
  export LOGMET_HOST=$(etcdctl get /logmet/host)
  export LOGMET_PORT=$(etcdctl get /logmet/port)
  export LOGMET_TENANT=$(etcdctl get /logmet/tenant)
  export LOGMET_PWD=$(etcdctl get /logmet/pwd)
  export SYSTEM_ID=$(etcdctl get /global/system_id)
  export KAFKA_SERVICE_URL=$(etcdctl get /kafka/url)
  export MESSAGEHUB_USER=$(etcdctl get /kafka/user)
  export MESSAGEHUB_PASSWORD=$(etcdctl get /passwords/kafka)

  #to run with message hub, we need a jaas jar we can only obtain
  #from github, and have to use an extra config snippet to enable it.
  cd /opt/ibm/wlp/usr/servers/defaultServer
  mkdir -p configDropins/overrides
  mv kafkaDropin.xml configDropins/overrides
  wget https://github.com/ibm-messaging/message-hub-samples/raw/master/java/message-hub-liberty-sample/lib-message-hub/messagehub.login-1.0.0.jar

  # Softlayer needs a logstash endpoint so we set up the server
  # to run in the background and the primary task is running the
  # forwarder. In ICS, Liberty is the primary task so we need to
  # run it in the foreground
  if [ "$LOGSTASH_ENDPOINT" != "" ]; then
    /opt/ibm/wlp/bin/server start defaultServer
    echo Starting the logstash forwarder...
    sed -i s/PLACEHOLDER_LOGHOST/$(etcdctl get /logstash/endpoint)/g /opt/forwarder.conf
    cd /opt
    chmod +x ./forwarder
    etcdctl get /logstash/cert > logstash-forwarder.crt
    etcdctl get /logstash/key > logstash-forwarder.key
    sleep 0.5
    ./forwarder --config ./forwarder.conf
  else
    /opt/ibm/wlp/bin/server run defaultServer
  fi
else
  echo A8_ENDPOINT_TYPE=${A8_ENDPOINT_TYPE}
  echo A8_ENDPOINT_PORT=${A8_ENDPOINT_PORT}
  exec a8sidecar --supervise /opt/ibm/wlp/bin/server run defaultServer
fi
