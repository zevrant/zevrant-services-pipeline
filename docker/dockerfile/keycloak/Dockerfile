FROM quay.io/keycloak/keycloak:19.0.3

RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true --metrics-enabled=true

ENTRYPOINT /opt/keycloak/bin/kc.sh start