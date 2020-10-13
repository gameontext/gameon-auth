FROM adoptopenjdk/openjdk8-openj9:ubi

COPY app.jar /
COPY startup.sh /

RUN mkdir /etc/certs && chmod 777 /etc/certs

CMD ["/startup.sh"]

# Generous start time for constrained systems
HEALTHCHECK \
  --timeout=10s \
  --start-period=2m \
  CMD curl -f http://localhost:9080/auth/health
