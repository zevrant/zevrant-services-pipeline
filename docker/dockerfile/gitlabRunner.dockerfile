FROM gitlab/gitlab-runner:alpine

RUN apk update \
	&& apk add docker \
	&& apk add openrc \
    && apk add jq \
	&& rc-update add docker boot \
	&& docker --version

RUN apk add --update \
    python \
    python-dev \
    py-pip \
    build-base \
    && pip install --upgrade --user awscli \
    && apk --purge -v del py-pip \
    && rm -rf /var/cache/apk/*

