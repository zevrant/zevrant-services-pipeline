#!/bin/bash

#Create Local SSL Keystore
export POD_IP=192.168.1.1
export POD_NAME=$HOSTNAME
password=$LOCAL_KEYSTORE_PASSWORD
bash ../bash/zevrant-services-start.sh localhost $password
sudo mkdir -p /storage/keys/
sudo cp ~/zevrant-services.p12 /storage/keys/
sudo chmod a+r /storage/keys/zevrant-services.p12

#Setup Kafka
docker-compose -f docker/kafka.yml up -d

#Setup Kafka Topics
kafka-topics.sh --create --topic zevrant-video-stream --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --config retention.ms=86400000
kafka-topics.sh --list --zookeeper localhost:2181


