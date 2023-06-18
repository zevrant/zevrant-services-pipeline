FROM harbor.zevrant-services.com/quay/keycloak/keycloak:latest

ENV KC_DB=postgres

RUN /opt/keycloak/bin/kc.sh build --health-enabled=true --metrics-enabled=true

ENTRYPOINT /opt/keycloak/bin/kc.sh start