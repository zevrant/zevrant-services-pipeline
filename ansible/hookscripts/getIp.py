#!/usr/bin/python3
import subprocess
import platform

## Choose first address in the CIDR block that fails to respond to a ping attempt

hostCidr = "172.16.2."
# hostCidr = "{{ hostCidr }}"
def failedPing(host):
  command = ['ping', '-c1', '-w1',  host]
  return subprocess.call(command) != 0

for i in range(1, 255):
  host = hostCidr + str(i)
  if failedPing(host):
    print(host)
    break