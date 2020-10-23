FROM adoptopenjdk/openjdk8-openj9:ubi

ENV ETCD_VERSION 2.2.2

# setup etcd
RUN curl -L -s -o etcd-v${ETCD_VERSION}-linux-amd64.tar.gz https://github.com/coreos/etcd/releases/download/v${ETCD_VERSION}/etcd-v${ETCD_VERSION}-linux-amd64.tar.gz \
  && tar xzf etcd-v${ETCD_VERSION}-linux-amd64.tar.gz etcd-v${ETCD_VERSION}-linux-amd64/etcdctl --strip-components=1 \
  && rm etcd-v${ETCD_VERSION}-linux-amd64.tar.gz \
  && mv etcdctl /usr/local/bin/etcdctl

COPY app.jar /
COPY startup.sh /

RUN mkdir /etc/certs && chmod 777 /etc/certs

CMD ["/startup.sh"]

# Generous start time for constrained systems
HEALTHCHECK \
  --timeout=10s \
  --start-period=2m \
  CMD curl -f http://localhost:9080/auth/health
