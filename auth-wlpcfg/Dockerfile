FROM gameontext/docker-liberty-custom

ENV SERVERDIRNAME auth

COPY ./startup.sh /opt/startup.sh
ADD ./servers/gameon-auth /opt/ol/wlp/usr/servers/defaultServer/

CMD ["/opt/startup.sh"]

HEALTHCHECK \
  --timeout=10s \
  --start-period=40s \
  CMD wget -q -O /dev/null http://localhost:9080/auth/health
