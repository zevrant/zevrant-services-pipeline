FROM containers.zevrant-services.com/zevrant/ubuntu-base:latest

RUN apt-get update \
    && apt-get upgrade -y \
    && useradd -m -d /opt/step-ca step-ca



#Install step-ca
RUN curl -L https://dl.step.sm/gh-release/cli/gh-release-header/v0.20.0/step-cli_0.20.0_amd64.deb -o /tmp/step-cli.deb \
    && ls -l /tmp \
    && dpkg -i /tmp/step-cli.deb \
    && curl -L https://dl.step.sm/gh-release/certificates/gh-release-header/v0.20.0/step-ca_0.20.0_amd64.deb -o /tmp/step-ca.deb \
    && dpkg -i /tmp/step-ca.deb \
    && step-ca version \
    && rm /tmp/step-ca.deb /tmp/step-cli.deb

Run apt-get install -y dnsutils

COPY start.sh /usr/local/bin/start.sh

RUN chmod a+rx /usr/local/bin/start.sh

user step-ca

ENTRYPOINT start.sh "$SECRET_FILE_PATH" "$CA_NAME"