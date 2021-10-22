FROM zevrant/zevrant-ubuntu-base:latest
USER root

RUN apt-get update \
    && apt-get upgrade -y

RUN useradd -m -d /var/lib/jenkins -G developers jenkins

USER jenkins

ENV NODEJS_HOME = /opt/nodejs
ENV JAVA_HOME /usr/lib/jvm/java-1.11.0-openjdk-amd64/
ENV PATH /usr/local/scripts:/usr/local/scripts/python:$PATH:$NODEJS_HOME/bin:$JAVA_HOME/bin

RUN aws --version
