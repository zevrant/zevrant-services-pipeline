#!/bin/bash

apt-get update
apt-get full-upgrade -y
apt-get install -y openssh-server

mkdir -p /home/zevrant/.ssh

cat << EOF >> /home/zevrant/.bashrc
alias edit="vim ~/.bashrc"
alias reload="source ~/.bashrc"
EOF

cat << EOF > /home/zevrant/.ssh/authorized_keys
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQD2Qc/s6P6wF3CDEglVdb20h+FbB2LQ1oRb06t9tAxwscJqsayh7rbGzKM0seMfEi5m/POIWNftqUHwCaALPFTel8/0Sx4tqbWVQEhqVoXG/1PSfI9kRriWHTX7gd+1VIhAcn0swOdcHNuivLMAeAIlqfkU/4zOTY/0vCJvbqPSoSVYq/+sQv4bX6Aw21naPsFGHDmaThJxp8rewK+qTNwgrNY8XGjCvdCmJeqVMNukAxlyZfvXxU4wPM8wfQiJ4V6OgDWY2Q7VaK2UO+EgSGWf4pfea4hWGpE34+OpWJ2R2boLaXHmE8u7G211IduAgNQ6w7pdUnYG1IdXZGKhmVyV gerethd@github/46316533 # ssh-import-id gh:gerethd
EOF

sed -i 's/PasswordAuthentication/#PasswordAuthentication/g' /etc/ssh/sshd_config
echo 'PasswordAuthentication no' >> /etc/ssh/sshd_config

echo 'zevrant ALL=(ALL:ALL) NOPASSWD: ALL' > /etc/sudoers.d/admin-users
chmod 0440 /etc/sudoers.d/admin-users

systemctl daemon-reload
systemctl restart sshd
