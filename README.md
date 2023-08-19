# Proxmox Useful Commands: 

| Command                                                                   | Description |
|---------------------------------------------------------------------------| ----- |
| dmsetup remove \`lsblk -r \| grep ceph \| awk '{print $1}' \| tail -n 1\` | List All Block Devices and remove holds on objects containing ceph in the name |
| pveceph purge | Purge Ceph cluster configuration, make sure to empty the contents of /etc/ceph/ceph.conf first |
| scp /var/var/lib/ceph/bootstrap-osd/ceph.keyring dest | This file contains the encryption key for creating OSDs. for OSDs that fail to create on hosts, copy this from good hosts|
