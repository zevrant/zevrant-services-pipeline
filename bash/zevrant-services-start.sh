sed -i "s/\${POD_IP}/$POD_IP/g" ~/openssl.conf
sed -i "s/\${SERVICE_NAME}/$1/g" ~/openssl.conf
echo "$3"
if [[ "$3" != "" ]]; then
  echo "applying additional ip $ADDITIONAL_IP"
  echo "		IP.2 = $3" >> ~/openssl.conf
fi
cat ~/openssl.conf

VAULT_ADDR="https://develop.vault.zevrant-services.internal"

if [[ "$ENVIRONMENT" == "prod" ]]; then
  VAULT_ADDR="https://vault.zevrant-services.internal"
fi

step ca bootstrap --ca-url certificate-authority.shared.svc.cluster.local --fingerprint 302e9a4e65cd9525a8479bc3bcd14c64050712559954e6cbf866469eba69fe37
openssl pkcs12 -export -inkey ~/private.pem -in ~/public.crt -passout "pass:$2" -out ~/zevrant-services.p12
rm ~/public.crt ~/private.pem ~/openssl.conf