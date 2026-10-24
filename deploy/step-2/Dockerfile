FROM eclipse-temurin:25-jre@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f
ARG SERVICE=booking-service
WORKDIR /app
COPY --chown=10001:0 source/${SERVICE}/target/quarkus-app/ /app/quarkus-app/
COPY source/booking-service/target/classes/com/milesofsmiles/booking/HealthProbe.class /opt/health/com/milesofsmiles/booking/HealthProbe.class
COPY --chmod=755 deploy/step-2/entrypoint.sh /app/entrypoint.sh
USER 10001:0
EXPOSE 8443
ENTRYPOINT ["/app/entrypoint.sh"]
