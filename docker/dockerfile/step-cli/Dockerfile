FROM containers.zevrant-services.com/ubuntu:latest

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y curl

#Install step-ca
RUN curl -L https://dl.step.sm/gh-release/cli/gh-release-header/v0.20.0/step-cli_0.20.0_amd64.deb -o /tmp/step-cli.deb \
    && ls -l /tmp \
    && dpkg -i /tmp/step-cli.deb \
    && rm /tmp/step-cli.deb
