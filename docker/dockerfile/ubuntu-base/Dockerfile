FROM containers.zevrant-services.com/zevrant/ubuntu:latest

RUN apt-get update\
    && apt-get upgrade -y


RUN DEBIAN_FRONTEND="noninteractive" apt-get -y install openjdk-17-jdk-headless jq curl awscli ca-certificates zip \
    && ln -sf /usr/lib/jvm/java-1.11.0-openjdk-amd64/ /usr/bin/java \
    && apt-get autoremove -y \
    && apt-get clean

COPY root_ca.crt /usr/local/share/ca-certificates/zevrant-services.crt

ENV JAVA_HOME /usr/lib/jvm/java-1.17.0-openjdk-amd64/

ENV PATH /usr/local/scripts:/usr/local/scripts/python:$JAVA_HOME/bin:$PATH:$NODEJS_HOME/bin

RUN groupadd --system developers \
    && echo y | update-ca-certificates \
    && keytool -importcert -file /usr/local/share/ca-certificates/zevrant-services.crt -alias zevrant-services-root -storepass changeit -keystore /usr/lib/jvm/java-1.17.0-openjdk-amd64/lib/security/cacerts

RUN curl -L https://dl.step.sm/gh-release/cli/gh-release-header/v0.20.0/step-cli_0.20.0_amd64.deb -o /tmp/step-cli.deb \
    && dpkg -i /tmp/step-cli.deb \
    && rm /tmp/step-cli.deb \
    && mkdir -p /.npmrc