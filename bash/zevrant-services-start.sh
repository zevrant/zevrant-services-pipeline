setAwsCredentials () {
  export AWS_ACCESS_KEY_ID=`echo $1 | jq .AccessKeyId`
  export AWS_SECRET_ACCESS_KEY=`echo $1 | jq .SecretAccessKey`
  if [ `echo "$1" | jq .Token` == "null" ];
  then
    export AWS_SESSION_TOKEN=`echo $1 | jq .SessionToken`
  else
    export AWS_SESSION_TOKEN=`echo $1 | jq .Token`
  fi
  AWS_ACCESS_KEY_ID=`echo $AWS_ACCESS_KEY_ID | cut -c 2-$((${#AWS_ACCESS_KEY_ID}-1))`
  AWS_SECRET_ACCESS_KEY=`echo $AWS_SECRET_ACCESS_KEY | cut -c 2-$((${#AWS_SECRET_ACCESS_KEY}-1))`
  AWS_SESSION_TOKEN=`echo $AWS_SESSION_TOKEN | cut -c 2-$((${#AWS_SESSION_TOKEN}-1))`
}

aws_credentials=`curl --proxy $PROXY_CREDENTIALS@3.210.165.61:3128 http://169.254.169.254/latest/meta-data/identity-credentials/ec2/security-credentials/ec2-instance`
setAwsCredentials "$aws_credentials"
credentials=`aws sts assume-role --role-arn arn:aws:iam::725235728275:role/SecretsOnlyServiceRole --role-session-name startup | jq .Credentials`
setAwsCredentials "$credentials"
curl https://raw.githubusercontent.com/zevrant/zevrant-services-pipeline/master/bash/openssl.conf > ~/openssl.conf
sed -i "s/\${POD_IP}/$POD_IP/g" ~/openssl.conf
sed -i "s/\${SERVICE_NAME}/$1/g" ~/openssl.conf
if [[ "$3" != "" ]]; then
  echo 'applying additional ip'
  echo "		IP.2 = $3" >> ~/openssl.confg
fi
cat ~/openssl.conf
openssl req -newkey rsa:4096 -nodes -keyout ~/private.pem -days 365 -out ~/public.csr -config ~/openssl.conf
username=`aws secretsmanager get-secret-value --region us-east-1 --secret-id certificateUsername | jq .SecretString`
password=`aws secretsmanager get-secret-value --region us-east-1 --secret-id certificatePassword | jq .SecretString`
username=`echo $username | cut -c 2-$((${#username}-1))`
password=`echo $password | cut -c 2-$((${#password}-1))`
certificateRequest=`cat ~/public.csr`
certificateRequest=`printf "%q" "$certificateRequest"`
certificateRequest=`echo $certificateRequest | cut -c 3-$((${#certificateRequest}-1))`
certificateRequest="{\"certificateRequest\":\"$certificateRequest\",\"ip\":\"$POD_IP\"}"
curl --insecure https://192.168.1.17:9009/zevrant-certificate-service/certs --data "$certificateRequest" --user $username:$password -H "Content-Type: application/json" -X POST > ~/public.crt
openssl pkcs12 -export -inkey ~/private.pem -in ~/public.crt -passout "pass:$2" -out ~/zevrant-services.p12
rm ~/public.crt ~/private.pem ~/openssl.conf
