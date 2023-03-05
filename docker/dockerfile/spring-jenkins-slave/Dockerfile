FROM docker.io/zevrant/zevrant-ubuntu-base:latest
USER root

RUN apt-get update \
    && apt-get full-upgrade -y \
    && apt-get install -y git python3-pip \
    && pip3 install awscli \
    && mkdir -p /var/zevrant-services/keystore/ \
    && mkdir -p /var/zevrant-services/vault/ \
    && mkdir -p /opt/acme/certs

RUN useradd -m -d /var/lib/jenkins -G developers jenkins \
    && mkdir -p /.npmrc \
    && chown -R 1000:1000 /.npmrc \
    && chown -R 1000:1000 /var/zevrant-services \
    && chmod -R 0700 /var/zevrant-services \
    && chown -R 1000:1000 /opt/acme \
    && chmod -R 0700 /opt/acme



USER jenkins

ENV JAVA_HOME /usr/lib/jvm/java-1.17.0-openjdk-amd64/
ENV PATH /usr/local/scripts:/usr/local/scripts/python:$PATH/bin:$JAVA_HOME/bin

RUN aws --version