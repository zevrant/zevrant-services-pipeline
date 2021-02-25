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
credentials=`aws sts assume-role --role-arn arn:aws:iam::725235728275:role/OauthServiceRole --role-session-name startup | jq .Credentials`
setAwsCredentials "$credentials"
openssl req -newkey rsa:4096 -nodes -keyout ~/private.pem -days 365 -out ~/public.csr -addext "subjectAltName = IP:$POD_IP" -subj "/C=US/ST=New York/L=Brooklyn/O=Example Brooklyn Company/CN=$POD_IP"

