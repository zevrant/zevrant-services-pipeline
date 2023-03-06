FROM docker.io/zevrant/ubuntu-base:latest
USER root

RUN apt-get install -y git

RUN useradd -m -d /var/lib/jenkins -G developers jenkins

USER jenkins

ENV JAVA_HOME /usr/lib/jvm/java-1.11.0-openjdk-amd64/
ENV PATH /usr/local/scripts:/usr/local/scripts/python:$PATH/bin:$JAVA_HOME/bin

RUN aws --version
