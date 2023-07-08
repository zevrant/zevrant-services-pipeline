FROM harbor.zevrant-services.com/zevrant-services/ubuntu-base:latest

RUN apt-get update \
    && apt-get install -y postgresql-client-14 \
    && psql --version \
    && useradd -m jenkins -u 10000
