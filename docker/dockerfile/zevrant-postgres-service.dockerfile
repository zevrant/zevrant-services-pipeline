FROM containers.zevrant-services.com/zevrant/postgres:latest

RUN echo "postgres ALL=(ALL) NOPASSWD: ALL" | tee -a /etc/sudoers

