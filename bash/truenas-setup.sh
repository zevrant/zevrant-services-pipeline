#!/bin/bash

apt-get install -y qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils net-tools
usermod -aG libvirt zevrant
usermod -aG libvirt-qemu zevrant
chown root:kvm /var/lib/libvirt/
curl 'https://download.truenas.com/TrueNAS-SCALE-Angelfish/22.02.0.1/TrueNAS-SCALE-22.02.0.1.iso' -O /var/lib/libvirt/

echo 'net.bridge.bridge-nf-call-ip6tables=0' >> /etc/sysctl.d/bridge.conf
echo 'net.bridge.bridge-nf-call-iptables=0' >> /etc/sysctl.d/bridge.conf
echo 'net.bridge.bridge-nf-call-arptables=0' >> /etc/sysctl.d/bridge.conf

echo 'ACTION=="add", SUBSYSTEM=="module", KERNEL=="br_netfilter", RUN+="/sbin/sysctl -p /etc/sysctl.d/bridge.conf"' >> /etc/udev/rules.d/99-bridge.rules

cat <<- EOF > /etc/netplan/00-installer-config.yaml
network:
  ethernets:
    eno1:
      dhcp4: true
    eno2:
      dhcp4: true
    eno3:
      dhcp4: true
    eno4:
      dhcp4: true
  bridges:
    br0:
      interfaces: [ eno1,eno2,eno3,eno4 ]
      addresses: [172.16.1.0/16]
      gateway4: 172.16.1.254
      dhcp4: yes
EOF

netplan apply
ip

cat <<- EOF > host-bridge.xml
<network>
  <name>host-bridge</name>
  <forward mode="bridge"/>
  <bridge name="br0"/>
</network>
EOF

virsh net-define host-bridge.xml
virsh net-start host-bridge
virsh net-autostart host-bridge
virsh net-list --all
