FROM centos:latest

RUN yum update -y

RUN yum -y install java-11-openjdk-devel python3 \
  && ln -sf /usr/lib/jvm/java/bin/java /usr/bin/java

ENV NODEJS_HOME = /opt/nodejs

RUN mkdir $NODEJS_HOME \
  && curl https://nodejs.org/dist/v12.13.1/node-v12.13.1-linux-x64.tar.xz -o node-v12.13.1-linux-x64.tar.xz \
  && tar -xf ./node-v12.13.1-linux-x64.tar.xz \
  && mv ./node-v12.13.1-linux-x64/* $NODEJS_HOME/

ENV JAVA_HOME /usr/lib/jvm/java-1.11.0-openjdk

ENV PATH /usr/local/scripts:/usr/local/scripts/python:/usr/lib/jvm/java-1.11.0-openjdk/bin/java:$PATH:$NODEJS_HOME/bin

RUN yum -y install unzip python3-pip wget

RUN pip3 install awscli

RUN aws --version

RUN groupadd --system developers
