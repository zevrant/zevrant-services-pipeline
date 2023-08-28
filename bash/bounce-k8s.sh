#!/bin/bash

nodes="$(kubectl get nodes --no-headers=true | grep worker | awk '{ print $1}')"

readarray array <<<"$nodes"

for i in "${!array[@]}"
do
  node="$(echo "${array[$i]}" | xargs)"
  kubectl cordon "$node"
  kubectl drain "$node" --ignore-daemonsets --force --delete-emptydir-data
  ipAddress="$(kubectl describe nodes "$node" | grep InternalIP: | awk '{ print $2 }')"
  ssh -o StrictHostKeyChecking=no "$ipAddress" sudo reboot
  sleep 5
  status=-1
  while [[ "$status" != "0" ]]; do
    sleep 5
    ssh -o StrictHostKeyChecking=no "$ipAddress" echo hello
    status="$(echo $?)"
  done
  kubectl uncordon "$node"
done