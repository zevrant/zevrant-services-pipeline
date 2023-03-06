FROM containers.zevrant-services.com/zevrant/keycloak/keycloak:18.0.2

RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true --metrics-enabled=true

ENTRYPOINT /opt/keycloak/bin/kc.sh start