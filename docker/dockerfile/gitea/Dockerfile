FROM docker.io/gitea/gitea:latest-rootless

USER root

COPY root_ca.crt /usr/local/share/ca-certificates/zevrant-services.pem

RUN update-ca-certificates

USER 1000:1000