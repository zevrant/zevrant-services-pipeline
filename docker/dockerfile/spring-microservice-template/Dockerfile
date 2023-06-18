FROM harbor.zevrant-services.com/zevrant-services/ubuntu-base:latest

ARG serviceName
ARG containerPort

EXPOSE $containerPort

RUN mkdir -p /usr/local/microservices/zevrant-home-services/$serviceName/ \
    && mkdir -p /var/log/zevrant-home-services/$serviceName\
    && mkdir -p /storage/keys \
    && useradd -m -d /usr/local/microservices/zevrant-home-services/$serviceName/ -G developers  $serviceName \
    && chown -R $serviceName:developers /var/log/zevrant-home-services/$serviceName /usr/local/microservices/zevrant-home-services/$serviceName /storage/keys

USER $serviceName

ENV SERVICE_NAME=$serviceName
ENV HOME=/usr/local/microservices/zevrant-home-services/${serviceName}/

COPY build/libs/${serviceName}-*.jar /usr/local/microservices/zevrant-home-services/${serviceName}/${serviceName}.jar

WORKDIR $HOME

ENV SLEEP_TIME 0

ENTRYPOINT sleep $SLEEP_TIME \
    && cp /opt/acme/certs/zevrant-services.p12 $HOME/zevrant-services.p12 \
    && /usr/bin/step ca bootstrap --ca-url certificate-authority --fingerprint $CA_FINGERPRINT \
    && java -jar -Dspring.profiles.active=$ENVIRONMENT ${SERVICE_NAME}.jar
