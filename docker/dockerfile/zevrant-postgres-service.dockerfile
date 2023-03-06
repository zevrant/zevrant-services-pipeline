FROM containers.zevrant-services.com/zevrant/postrges:latest

RUN echo "postgres ALL=(ALL) NOPASSWD: ALL" | tee -a /etc/sudoers

