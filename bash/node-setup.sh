#!/bin/bash

## Install Updates and dependencies
apt-get update
apt-get full-upgrade -y
apt-get install -y jq awscli curl
groupadd developers
##Install Node Exporter (Prometheus)
mkdir -p /opt/node-exporter
url="$(curl -s https://api.github.com/repos/prometheus/node_exporter/releases/latest | jq .assets[2].browser_download_url)"
curl -L $(echo "$url" | cut -c 2-$((${#url}-1))) -o node-exporter.tar.gz
tar xvf node-exporter.tar.gz
mv node_exporter-* node-exporter
mv node-exporter/* /opt/node-exporter
rm -r node-exporter*
adduser --system --shel /bin/false --no-create-home --disabled-login node-exporter

#Generate SSL certificate, MUST BE REFRESHED EVERY 8 DAYS!!!!!
myIp=""
read -ra "output" <<< "$(hostname -I)"
for ip in "${output[@]}"
do
  if [[ "$ip" == *"192.168.1"* ]]; then
    myIp=$ip
    break;
  fi
done

curl https://raw.githubusercontent.com/zevrant/zevrant-services-pipeline/master/bash/openssl.conf > ./openssl.conf
sed -i "s/\${POD_IP}/$myIp/g" ./openssl.conf
sed -i "s/\${SERVICE_NAME}/$(hostname)/g" ./openssl.conf
echo "$3"

openssl req -newkey rsa:4096 -nodes -keyout /opt/node-exporter/private.pem -days 8 -out ./public.csr -config ./openssl.conf
echo 'Enter AWS Access Key Id.'
read AWS_ACCESS_KEY_ID
echo 'Enter AWS Secret Access Key'
read AWS_SECRET_ACCESS_KEY

mkdir -p ~/.aws/

cat << EOF > ~/.aws/credentials
[default]
aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
aws_access_key_id = $AWS_ACCESS_KEY_ID
EOF

cat << EOF > ~/.aws/config
[default]
region = us-east-1
output = json

EOF


username=$(aws secretsmanager get-secret-value --region us-east-1 --secret-id certificateUsername | jq .SecretString)
echo $username
password=$(aws secretsmanager get-secret-value --region us-east-1 --secret-id certificatePassword | jq .SecretString)
username=$(echo "$username" | cut -c 2-$((${#username}-1)))
password=$(echo "$password" | cut -c 2-$((${#password}-1)))

rm -rf ~/.aws/config

certificateRequest=$(cat ./public.csr)
certificateRequest=$(printf "%q" "$certificateRequest")
certificateRequest=$(echo "$certificateRequest" | cut -c 3-$((${#certificateRequest}-1)))
certificateRequest="{\"certificateRequest\":\"$certificateRequest\",\"ip\":\"$myIp\"}"
curl --insecure https://192.168.1.17:9009/zevrant-certificate-service/certs --data "$certificateRequest" \
  --user "$username":"$password" -H "Content-Type: application/json" -X POST -o /opt/node-exporter/public.crt
rm openssl.conf public.csr
cat << EOF > /etc/systemd/system/node-exporter.service
[Unit]
Description=Node Exporter
Documentation=

Wants=network.target
After=network.target

[Service]
User=node-exporter

WorkingDirectory=/opt/node-exporter
ExecStart=/opt/node-exporter/node_exporter --web.config=/opt/node-exporter/config.yml

[Install]
WantedBy=multi-user.target
EOF

cat << EOF > /opt/node-exporter/config.yml
tls_server_config:
  cert_file: public.crt
  key_file: private.pem
EOF

chown -R node-exporter:developers /opt/node-exporter

systemctl daemon-reload
systemctl start node-exporter
systemctl enable node-exporter
