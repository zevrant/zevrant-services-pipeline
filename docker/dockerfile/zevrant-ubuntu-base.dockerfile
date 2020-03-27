FROM ubuntu:latest

RUN apt-get update\
  && apt-get upgrade -y

RUN apt-get -y install openjdk-11-jdk python3 python3-pip wget iproute2 net-tools\
  && ln -sf /usr/lib/jvm/java-1.11.0-openjdk-amd64/ /usr/bin/java

ENV NODEJS_HOME = /opt/nodejs

RUN mkdir $NODEJS_HOME \
  && wget https://nodejs.org/dist/v12.13.1/node-v12.13.1-linux-x64.tar.xz\
  && tar -xf ./node-v12.13.1-linux-x64.tar.xz \
  && mv ./node-v12.13.1-linux-x64/* $NODEJS_HOME/

ENV JAVA_HOME /usr/lib/jvm/java-1.11.0-openjdk-amd64/

ENV PATH /usr/local/scripts:/usr/local/scripts/python:$JAVA_HOME/bin:$PATH:$NODEJS_HOME/bin

RUN pip3 install awscli

RUN aws --version

RUN groupadd --system developers
