FROM containers.zevrant-services.com/jenkins/jenkins:2.375.1-lts-jdk11

USER root

COPY root_ca.crt /usr/local/share/ca-certificates/zevrant-services.crt

RUN echo y | update-ca-certificates \
    && echo yes | keytool -importcert -file /usr/local/share/ca-certificates/zevrant-services.crt -alias zevrant-services-root -storepass changeit -keystore /opt/java/openjdk/lib/security/cacerts

