FROM prom/prometheus:latest

ADD prometheus.yml /etc/prometheus/
ADD rules.yml /etc/prometheus
