FROM zevrant/zevrant-ubuntu-base:latest

ARG apiLevel

RUN echo $apiLevel \
    && apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y –-no-install-recommends wget zip qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils

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
    && ls -la /opt/android/cmdline-tools/latest/bin

ENV ANDROID_SDK_ROOT=/opt/android
#deprecated but some stuff still may use it
ENV ANDROID_HOME=/opt/android

#Set path for downloaded tools
ENV PATH=/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/opt/android/emulator:/opt/android/platform-tools/:/opt/android/cmdline-tools/latest/bin/:/opt/android/build-tools/31.0.0/

#install sdk tools & emulator packages
RUN sdkmanager --update \
    && echo y | sdkmanager platform-tools "platforms;android-30" "platforms;android-29" "platforms;android-28" "build-tools;31.0.0" "system-images;android-30;google_apis;x86_64" "system-images;android-29;google_apis;x86_64" "system-images;android-28;google_apis;x86_64"


