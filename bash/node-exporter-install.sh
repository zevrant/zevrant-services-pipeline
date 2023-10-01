rm -rf /opt/node-exporter/*
url='https://github.com/prometheus/node_exporter/releases/download/v1.6.1/node_exporter-1.6.1.linux-amd64.tar.gz'
curl -L "$url" -o node-exporter.tar.gz
tar xvf node-exporter.tar.gz
mkdir -p /opt/node-exporter
mv node_exporter-* node-exporter
mv node-exporter/* /opt/node-exporter
rm -rf node-exporter*
useradd --system -u 997 node-exporter
chown -R node-exporter:node-exporter /opt/node-exporter
rm -f /etc/systemd/system/node-exporter.service
echo W1VuaXRdCkRlc2NyaXB0aW9uPU5vZGUgRXhwb3J0ZXIKRG9jdW1lbnRhdGlvbj0KCldhbnRzPW5ldHdvcmsudGFyZ2V0CkFmdGVyPW5ldHdvcmsudGFyZ2V0CgpbU2VydmljZV0KVXNlcj1ub2RlLWV4cG9ydGVyCgpXb3JraW5nRGlyZWN0b3J5PS9vcHQvbm9kZS1leHBvcnRlcgpFeGVjU3RhcnQ9L29wdC9ub2RlLWV4cG9ydGVyL25vZGVfZXhwb3J0ZXIKCltJbnN0YWxsXQpXYW50ZWRCeT1tdWx0aS11c2VyLnRhcmdldAo= | base64 -d > /usr/lib/systemd/system/node-exporter.service
ausearch -c '(exporter)' --raw | audit2allow -M my-exporter
semodule -X 300 -i my-exporter.pp
restorecon -R /opt/node-exporter
systemctl enable --now node-exporter
systemctl status node-exporter