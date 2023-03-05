#!/bin/bash

CA_NAME="$2"
PASSWORD_FILE_PATH="$1"

echo "CA NAME is $CA_NAME"
echo "PASSWORD_FILE_PATH is $PASSWORD_FILE_PATH"

if [[ ! -d "/opt/step-ca/.step/authorities/${CA_NAME}" ]]; then
  step ca init --authority="$CA_NAME" --dns=certificate-authority --dns  --deployment-type=standalone -name "$CA_NAME" --address='0.0.0.0:8443' --provisioner="$CA_NAME" --password-file="$PASSWORD_FILE_PATH"
  rm -f "/opt/step-ca/.step/authorities/${CA_NAME}/secrets/root_ca_key"
  step ca provisioner add acme --type ACME --ca-url "$CA_DNS"
fi



step-ca "/opt/step-ca/.step/authorities/${CA_NAME}/config/ca.json" --password-file="$PASSWORD_FILE_PATH"