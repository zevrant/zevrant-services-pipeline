FROM ubuntu:latest

RUN apt-get update && apt-get full-upgrade -y && apt-get install -y wget

RUN mkdir -p /etc/prometheus /var/lib/prometheus \
    && useradd --no-create-home --shell /bin/false prome \
    && useradd --no-create-home --shell /bin/false node_exporter

RUN wget https://github.com/prometheus/prometheus/releases/download/v2.27.1/prometheus-2.27.1.linux-amd64.tar.gz \
    && tar xvf prometheus-2.27.1.linux-amd64.tar.gz \
    && cp prometheus-2.27.1.linux-amd64/prometheus /usr/local/bin/ \
    && cp prometheus-2.27.1.linux-amd64/promtool /usr/local/bin/ \
    && chown prome:prome /usr/local/bin/prometheus \
    && cp -r prometheus-2.27.1.linux-amd64/consoles /etc/prometheus \
    && cp -r prometheus-2.27.1.linux-amd64/console_libraries /etc/prometheus \
    && chown -R prome:prome /etc/prometheus/consoles \
    && chown -R prome:prome /etc/prometheus/console_libraries \
    && rm -rf prometheus-2.27.1.linux-amd64.tar.gz prometheus-2.27.1.linux-amd64/


ADD prometheus.yml /etc/prometheus/
ADD rules.yml /etc/prometheus

CMD /usr/local/bin/prometheus \
        --config.file /etc/prometheus/prometheus.yml \
        --storage.tsdb.path /var/lib/prometheus/ \
        --web.console.templates=/etc/prometheus/consoles \
        --web.console.libraries=/etc/prometheus/console_libraries