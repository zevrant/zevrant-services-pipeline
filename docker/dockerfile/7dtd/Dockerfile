FROM containers.zevrant-services.com/zevrant/cm2network/steamcmd:latest

RUN /home/steam/steamcmd/steamcmd.sh +login anonymous +app_update 294420 +quit
USER root

RUN mkdir -p /opt/7dtdconfig/ \
    && chown -R steam:steam /opt/7dtdconfig/

USER steam

WORKDIR /home/steam/Steam/steamapps/common/7\ Days\ to\ Die\ Dedicated\ Server/

RUN cp serverconfig.xml /opt/7dtdconfig/serverconfig.xml

ENTRYPOINT ./startserver.sh -configfile=/opt/7dtdconfig/serverconfig.xml