FROM adoptopenjdk/openjdk8-openj9:ubi

COPY app.jar /
COPY startup.sh /

RUN mkdir /etc/certs && chmod 777 /etc/certs

HEALTHCHECK --timeout=10s --start-period=40s CMD wget -q -O /dev/null http://localhost:9080/auth/health

CMD ["/startup.sh"]