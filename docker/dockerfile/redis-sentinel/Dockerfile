FROM docker.io/ubuntu:latest

RUN apt-get update \
    && apt-get dist-upgrade -y \
    && apt-get install -y redis-sentinel

RUN redis-sentinel --version