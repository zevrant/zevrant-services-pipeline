FROM docker.io/zevrant/ubuntu-base:latest

COPY inbound-agent.sh /usr/local/bin/jenkins-agent

RUN apt-get update \
    && apt-get full-upgrade -y \
    && apt-get install -y git git-lfs fontconfig zip \
    && apt-get clean \
    && useradd -m -d /home/jenkins -r -u 10000 jenkins \
    && chmod a+rx /usr/local/bin/jenkins-agent \
    && curl 'https://repo.jenkins-ci.org/api/search/versions?g=org.jenkins-ci.main&a=remoting&repos=releases' | jq -r '.[] | map(select(.version))'[0].version > /tmp/version \
    && curl --create-dirs -fsSLo /usr/share/jenkins/agent.jar https://repo.jenkins-ci.org/public/org/jenkins-ci/main/remoting/"$(cat /tmp/version)"/remoting-"$(cat /tmp/version)".jar \
    && chmod 755 /usr/share/jenkins \
    && chmod 644 /usr/share/jenkins/agent.jar \
    && chown -R jenkins:jenkins /usr/share/jenkins \
    && chown -R jenkins:jenkins /home/jenkins/ \
    && ln -sf /usr/share/jenkins/agent.jar /usr/share/jenkins/slave.jar

USER 10000

ENV HOME=/home/jenkins
WORKDIR /home/jenkins

RUN keytool -import -alias zevrant-services-ca -file /usr/local/share/ca-certificates/zevrant-services.crt -keystore truststore.jks -storepass changeit

ENTRYPOINT ["bash", "-c", "/usr/local/bin/jenkins-agent"]