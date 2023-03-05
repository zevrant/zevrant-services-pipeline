sleep 26
step ca bootstrap --ca-url certificate-authority --force --fingerprint $CA_FINGERPRINT --install
step ca bootstrap --ca-url certificate-authority.develop.svc.cluster.local --force --fingerprint $CA_FINGERPRINT --install
update-ca-certificates
echo "Starting unsealer"
status=""
while true 
do 
    echo $URL  
    status=$(curl https://${URL}/v1/sys/seal-status)
    echo $status 
    status=$(echo $status | jq .sealed) 
    if [ true == "$status" ]; 
    then 
        echo "Unsealing"
        curl -i --request PUT --data @/var/zevrant-services/vault-keys/vault-key-1 https://${URL}/v1/sys/unseal
        curl -i --request PUT --data @/var/zevrant-services/vault-keys/vault-key-2 https://${URL}/v1/sys/unseal
        curl -i --request PUT --data @/var/zevrant-services/vault-keys/vault-key-3 https://${URL}/v1/sys/unseal
        status=$(curl https://${URL}/v1/sys/seal-status | jq .sealed)
        echo $status
    fi 
    sleep 10 
done
