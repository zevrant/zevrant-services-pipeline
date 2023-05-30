FROM harbor.zevrant-services.com/dockerhub/cm2network/steamcmd:latest

USER root
RUN apt-get update \
    && apt-get dist-upgrade -y \
    && apt-get clean \
    && mkdir -p /opt/7dtdconfig/ \
    && chown -R steam:steam /opt/7dtdconfig/

USER steam

RUN /home/steam/steamcmd/steamcmd.sh +login anonymous +app_update 294420 +quit

WORKDIR /home/steam/Steam/steamapps/common/7\ Days\ to\ Die\ Dedicated\ Server/

RUN cp serverconfig.xml /opt/7dtdconfig/serverconfig.xml

ENTRYPOINT ./startserver.sh -configfile=./serverconfig.xml