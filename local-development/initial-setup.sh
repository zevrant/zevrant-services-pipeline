#!/bin/bash

docker-compose -f docker/kafka.yml up -d

kafka-topics.sh --create --topic zevrant-video-stream --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --config retention.ms=86400000
kafka-topics.sh --list --zookeeper localhost:2181


