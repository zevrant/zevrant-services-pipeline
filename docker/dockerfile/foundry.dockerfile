FROM ubuntu:latest

EXPOSE 30000

RUN apt-get update; apt-get full-upgrade -y; apt-get install -y curl

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -; apt-get install -y nodejs

RUN useradd sdp;\
  usermod -u 1002 sdp;

RUN mkdir -p /home/sdp/.local/share/;\
    mkdir -p /opt/foundry/data;\
    mkdir -p /home/sdp/.local/share/FoundryVTT;\
    chown -R sdp:sdp /opt/foundry/data;

COPY ./ /opt/foundry/

USER sdp

CMD node /opt/foundry/resources/app/main.js