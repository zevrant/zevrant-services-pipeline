FROM ubuntu:latest

EXPOSE 30000

RUN apt-get update; apt-get full-upgrade -y; apt-get install -y curl

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -; apt-get install -y nodejs

RUN mkdir -p /opt/foundry/data; mkdir -p /root/.local/share/FoundryVTT;

COPY ./ /opt/foundry/

RUN ls -l /opt/foundry

CMD ls -l /opt/foundry/resources/app; node /opt/foundry/resources/app/main.js