FROM ubuntu:latest

RUN apt-get update\ 
    && apt-get full-upgrade -y

RUN apt-get install -y openssh-server

RUN sed -i "s/Port 22/Port 2222/g" /etc/ssh/sshd_config

ENTRYPOINT /usr/sbin/sshd -D
