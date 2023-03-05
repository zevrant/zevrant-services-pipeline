FROM docker.io/octofarm/octofarm:latest

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y curl \
    && apt-get clean \
    && chown -R node:node /app \
    && curl 'http://zevrant-01.zevrant-services.com:7644/cacert.pem' -o /usr/local/share/ca-certificates/zevrant-services-ca-root.crt \
    && update-ca-certificates

USER node

RUN cp /usr/local/share/ca-certificates/zevrant-services-ca-root.crt /app

ENV NODE_EXTRA_CA_CERTS=/app/zevrant-services-ca-root.crt
