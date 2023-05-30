FROM harbor.zevrant-services.com/dockerhub/ubuntu:latest

RUN DEBIAN_FRONTEND="noninteractive" apt-get update \
    && apt-get -y install openjdk-17-jdk-headless curl zip

#Install Server
RUN mkdir /opt/minecraft \
    && curl -o /opt/minecraft/minecraft-server.zip https://mediafilez.forgecdn.net/files/4536/699/Server-Files-1.0.18.zip \
    && unzip /opt/minecraft/minecraft-server.zip -d /opt/minecraft/ \
    && mv /opt/minecraft/Server*/* /opt/minecraft/ \
    && rm -r /opt/minecraft/minecraft-server.zip /opt/minecraft/Server*

RUN useradd -m minecraft -u 10001 \
    && echo 'eula=true' >> /opt/minecraft/eula.txt \
    && chown -R minecraft /opt/minecraft \
    && chmod +x /opt/minecraft/startserver.sh

USER minecraft

ENTRYPOINT /opt/minecraft/startserver.sh