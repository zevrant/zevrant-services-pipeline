FROM grafana/grafana:latest

COPY ./public.crt /etc/ssl/cert.pem