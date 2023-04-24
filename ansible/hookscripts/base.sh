#!/bin/bash

VM_ID=$1;
EXECUTION_PHASE=$2

#if [[ "$EXECUTION_PHASE" == 'post-start' ]]; then

  println "Performing updates on vm $VM_ID"
  echo 'hello from hookscript' > /home/zevrant/hello-world

#fi

#exit 0
