FROM docker.io/zevrant/zevrant-ubuntu-base:latest

RUN apt-get update \
    && apt-get install -y apt-transport-https ca-certificates \
    && curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | tee /etc/apt/sources.list.d/kubernetes.list \
    && apt-get update \
    && apt-get install -y kubectl

RUN useradd -m -d /var/lib/jenkins -G developers jenkins

USER jenkins

RUN kubectl
