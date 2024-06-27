ARG APP_INSIGHTS_AGENT_VERSION=3.5.2

# Application image
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/reform-scan-notification-service.jar /opt/app/


EXPOSE 8585
CMD [ "reform-scan-notification-service.jar" ]
