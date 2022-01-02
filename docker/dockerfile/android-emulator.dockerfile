FROM docker.io/ubuntu:latest

ARG sdkVersion=31

ENV SDK_VERSION=$sdkVersion

RUN groupadd --system developers

RUN apt-get update \
    && apt-get upgrade -y \
    && DEBIAN_FRONTEND="noninteractive" apt-get -y install openjdk-11-jdk python3 python3-pip wget iproute2 net-tools jq curl wget zip qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils \
    && ln -sf /usr/lib/jvm/java-1.11.0-openjdk-amd64/ /usr/bin/java \
    && apt-get install -y wget zip qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils \
    && apt-get clean

#download android sdk cmdline tools
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip -O cmdline-tools.zip \
    && mkdir -p /opt/android \
    && unzip cmdline-tools.zip \
    && mv cmdline-tools /opt/android/ \
    && mkdir -p /opt/android/cmdline-tools/latest/ \
    && mv /opt/android/cmdline-tools/bin /opt/android/cmdline-tools/latest/bin \
    && mv /opt/android/cmdline-tools/source.properties /opt/android/cmdline-tools/latest/source.properties \
    && mv /opt/android/cmdline-tools/lib /opt/android/cmdline-tools/latest/lib \
    && mv /opt/android/cmdline-tools/NOTICE.txt /opt/android/cmdline-tools/latest/NOTICE.txt \
    && chown -R root:developers /opt/android

ENV ANDROID_SDK_ROOT=/opt/android
#deprecated but some stuff still may use it
ENV ANDROID_HOME=/opt/android

ENV JAVA_HOME /usr/lib/jvm/java-1.11.0-openjdk-amd64/

#Set path for downloaded tools
ENV PATH=$PATH:/usr/local/scripts:/usr/local/scripts/python:$JAVA_HOME/bin:/opt/android/emulator:/opt/android/platform-tools/:/opt/android/cmdline-tools/latest/bin/:/opt/android/build-tools/31.0.0/

RUN pip3 install awscli

#install sdk tools & emulator packages
RUN sdkmanager --update \
    && echo y | sdkmanager platform-tools "platforms;android-31" \
    && echo y | sdkmanager platform-tools "emulator" "build-tools;31.0.0" \
    && echo y | sdkmanager "system-images;android-$SDK_VERSION;google_apis;x86_64"
#RUN rm -rf /opt/android/emulator/ \
#    && cp -r /opt/android/emulator-2/ /opt/android/emulator/

RUN apt-get install -y locales \
    && locale-gen "en_US.UTF-8" \
    && update-locale LC_ALL="en_US.UTF-8"

RUN useradd -m -d /var/lib/jenkins -G developers -G kvm jenkins \
    && chmod g+rwx /opt/android /opt/android/emulator/ /opt/android/cmdline-tools/ \
    && chmod -R g+rw /opt/android \
    && chown -R root:developers /opt/android/

USER jenkins

RUN export LANG="en_US.UTF-8"

ENTRYPOINT bash
