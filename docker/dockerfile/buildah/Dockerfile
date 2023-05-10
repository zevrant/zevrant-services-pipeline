FROM docker.io/zevrant/buildah/stable:latest

COPY root_ca.crt /etc/pki/ca-trust/source/anchors/zevrant-services.crt

RUN update-ca-trust