FROM registry.gitlab.com/fdroid/docker-executable-fdroidserver:master

ARG buildtime_variable=30

ENV ANDROID_VERSION=$buildtime_variable

RUN apt-get update \
    && apt-get full-upgrade -y \
    && mkdir -p /fdroidserver \
    && useradd -m -d /fdroidserver/ fdroid -k UID_MIN=5000 -k UID_MAX=9999 \
    && chown -R fdroid:fdroid /fdroidserver

USER fdroid

RUN mkdir /fdroidserver/android \
    && curl https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip -o /fdroidserver/android/commandlinetools-linux-7583922_latest.zip

RUN unzip /fdroidserver/android/commandlinetools-linux-7583922_latest.zip -d /tmp \
    && ls -l /tmp/cmdline-tools \
    && mkdir -p /fdroidserver/android/cmdline-tools/latest \
    && mv /tmp/cmdline-tools/* /fdroidserver/android/cmdline-tools/latest \
    && echo 'export ANDROID_HOME="/fdroidserver/android"' >> /fdroidserver/.bashrc \
    && echo 'export ANDROID_SDK_HOME="/fdroidserver/android"' >> /fdroidserver/.bashrc \
    && echo 'export PATH=\$PATH:\$ANDROID_SDK_HOME/cmdline-tools/bin' >> /fdroidserver/.bashrc \
    && /fdroidserver/android/cmdline-tools/latest/bin/sdkmanager --version \
    && /fdroidserver/android/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-$ANDROID_VERSION" \
    && echo y | /fdroidserver/android/cmdline-tools/latest/bin/sdkmanager --licenses \
    && /fdroidserver/android/cmdline-tools/latest/bin/sdkmanager --update

RUN mkdir -p /repo

WORKDIR /repo/

ENTRYPOINT ["../fdroidserver/fdroid"]
CMD ["--help"]

