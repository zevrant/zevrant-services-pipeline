FROM postgres:latest

RUN echo "postgres ALL=(ALL) NOPASSWD: ALL" | tee -a /etc/sudoers

