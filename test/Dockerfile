FROM fedora:latest
#This is craeted for use with jenkins but should be a good example for setting up sshd in
#an openshift environment. This hinges on setting the resources needed by sshd to the random id
#that is assigned at runtime by the openshift environment. Docker user command does not work in openshift.
RUN dnf -y install openssh-server git
RUN dnf -y install ed # needed to edit passwd and group
RUN dnf clean all
# setup openssh
RUN sed -i "s/#PasswordAuthentication yes/PasswordAuthentication no/" /etc/ssh/sshd_config
# SSHd 7.4+ (maybe earlier) this is not needed, see
#  https://lists.mindrot.org/pipermail/openssh-unix-dev/2017-August/036168.html
RUN sed -i 's/#Port.*$/Port 2222/' /etc/ssh/sshd_config
RUN chmod 775 /var/run
RUN rm -f /var/run/nologin
# setup jenkins user
RUN adduser --system -s /bin/bash -u 1234321 -g 0 jenkins # uid to replace later
RUN chmod 775 /etc/ssh /home # keep writable for openshift user group (root)
RUN chmod 660 /etc/ssh/sshd_config
RUN chmod 664 /etc/passwd /etc/group # to help uid fix
RUN dnf upgrade -y
RUN dnf install curl gnupg2 wget apt-transport-https java-11-openjdk-devel.x86_64 ruby -y
    # Repo for CloudFoundry
RUN wget -O /etc/yum.repos.d/cloudfoundry-cli.repo https://packages.cloudfoundry.org/fedora/cloudfoundry-cli.repo\
    && yum install -y cf7-cli\
    && cf --help
# Install flyway binary
RUN wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/5.2.4/flyway-commandline-5.2.4-linux-x64.tar.gz \
    | tar xvz \
    && mv flyway-5.2.4 /usr/local/bin/flyway-5.2.4 \
    && ln -s /usr/local/bin/flyway-5.2.4/flyway /usr/local/bin \
    && chmod -R +x /usr/local/bin/flyway*

RUN ln -s /usr/lib/jvm/java-11 /jdk-11 \
    && /jdk-11/bin/java --version
# # Install pact broker
 RUN gem install pact_broker-client

RUN yum install -y python3-pip gcc \
    && pip3 install azure-cli

 RUN dnf module install -y nodejs:10\
     && node --version | grep v10.
 RUN dnf install -y jq maven netcat
 RUN npm install -g yaml-cli rimraf npm@6.2.0 npm-cli-login sql-cli \
     && npm config set unsafe-perm=true

RUN yum install bind* -y \
    && sed -i 's/listen/Port 2222/' /etc/ssh/sshd_config

RUN curl -o /etc/yum.repos.d/msprod.repo https://packages.microsoft.com/config/rhel/7/prod.repo \
    && dnf makecache \
    && ACCEPT_EULA=Y dnf install -y mssql-tools \
    && find / -name mssql \
    && /usr/local/bin/mssql --version \
    && ln -s /usr/local/bin/mssql /usr/bin/mssql \
    && /usr/bin/mssql --version

EXPOSE 2222
# RUN dnf install traceroute ping -y
USER jenkins
# CMD ["/usr/sbin/sshd", "-D"]
# FYI sed -i uses a temporary fail which approach fails
#CMD echo -e ",s/1234321/`id -u`/g\\012 w" | ed -s /etc/passwd && \
#    mkdir -p /home/jenkins/.gradle/ && \
#    touch /home/jenkins/.gradle/gradle.properties && \
#    chmod 0700 /home/jenkins/.gradle && \
#    chmod 0600 /home/jenkins/.gradle/gradle.properties && \
#    echo $'org.gradle.daemon = true \n\
#               systemProp.http.proxyHost=internet.ford.com \n\
#               systemProp.http.proxyPort=83 \n\
#               systemProp.https.proxyHost=internet.ford.com \n\
#               systemProp.https.proxyPort=83 \n\
#               systemProp.http.nonProxyHosts=localhost|internet.ford.com|*.ford.com //This entry will prevent the 503 Nexus service unreachable error' > /home/jenkins/.gradle/gradle.properties && \
#    mkdir -p /home/jenkins/.ssh && \
#    touch /home/jenkins/.ssh/authorized_keys && \
#    chmod 700 /home/jenkins/.ssh && \
#    chmod 600 /home/jenkins/.ssh/authorized_keys && \
#    echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC26UOsxSFy4pFMMovg8SnQEBeEo+HicNS12tjiiANR71qg3a7d4KPj69w53v9n34jS63CFwl8HGwmShdJ4OpOw2MoSNr7WoT4oA3cW+2IB294HUHctE0185G+4U/+5FJ5u10Al+YBX0lOtvmF8VFxtQ9BGPert1hLwZBm7CJCbSPaT2o6A73opJMW9ecj+R3pqqd9UQZCrlWm/0/yJ6eYMYEyaGKnaAqfPENUyVhf7Y/jYWSmAEuAroqHf9oQLImYR94OGnRfURg4k4B2c28bF+twQxrjc9ZWsv+Be5wnHpis1d5LuSR/mJzJNRNHzozSQQ4bojjkqKorGfu+5yd/fyDzSAd82n8kn5LuFLNtp/fGEETsKjoEQ87fm17dQfvJYlcr4N0unGOcGofQmlicBiB79uoSoRRPXgvwJP7XfHjDPAilcQ/cOBUZUO4V014QIstjNbj6jazT7xIMP2y4fP9EN7ToyoWwNIE3fJmesDHiluuUi7u4FJgm/OdYKu4s= gdittri1@MGC12D93X6MD6R.fbpld77.ford.com" > /home/jenkins/.ssh/authorized_keys && \
#    ssh-keygen -A && \
#    exec /usr/sbin/sshd -D

CMD cat /etc/resolv.conf \
    && echo -e ",s/1234321/`id -u`/g\\012 w" | ed -s /etc/passwd \
    && mkdir -p /home/jenkins/.gradle/ \
    && touch /home/jenkins/.gradle/gradle.properties \
    && chmod 0700 /home/jenkins/.gradle \
    && chmod 0600 /home/jenkins/.gradle/gradle.properties \
    && echo $'org.gradle.daemon=false\n\
    systemProp.http.proxyHost=internet.ford.com\n\
    systemProp.http.proxyPort=83\n\
    systemProp.http.nonProxyHosts=*.ford.com|localhost|internet.ford.com\n\
    systemProp.https.proxyHost=internet.ford.com\n\
    systemProp.https.proxyPort=83\n\
    systemProp.https.nonProxyHosts=*.ford.com|localhost|internet.ford.com //This entry will prevent the 503 Nexus service unreachable error' > /home/jenkins/.gradle/gradle.properties \
    && mkdir -p /home/jenkins/.ssh \
    && chmod 700 /home/jenkins/.ssh \
    && touch /home/jenkins/.ssh/authorized_keys \
    && chmod 600 /home/jenkins/.ssh/authorized_keys \
    && echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC26UOsxSFy4pFMMovg8SnQEBeEo+HicNS12tjiiANR71qg3a7d4KPj69w53v9n34jS63CFwl8HGwmShdJ4OpOw2MoSNr7WoT4oA3cW+2IB294HUHctE0185G+4U/+5FJ5u10Al+YBX0lOtvmF8VFxtQ9BGPert1hLwZBm7CJCbSPaT2o6A73opJMW9ecj+R3pqqd9UQZCrlWm/0/yJ6eYMYEyaGKnaAqfPENUyVhf7Y/jYWSmAEuAroqHf9oQLImYR94OGnRfURg4k4B2c28bF+twQxrjc9ZWsv+Be5wnHpis1d5LuSR/mJzJNRNHzozSQQ4bojjkqKorGfu+5yd/fyDzSAd82n8kn5LuFLNtp/fGEETsKjoEQ87fm17dQfvJYlcr4N0unGOcGofQmlicBiB79uoSoRRPXgvwJP7XfHjDPAilcQ/cOBUZUO4V014QIstjNbj6jazT7xIMP2y4fP9EN7ToyoWwNIE3fJmesDHiluuUi7u4FJgm/OdYKu4s= gdittri1@MGC12D93X6MD6R.fbpld77.ford.com" > /home/jenkins/.ssh/authorized_keys \
    && ssh-keygen -A \
    && exec /usr/sbin/sshd -D
