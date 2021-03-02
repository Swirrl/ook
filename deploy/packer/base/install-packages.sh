#!/bin/bash

set -e #exit on error

echo '>> INSTALL PACKAGES'

# install basics
sudo apt-get update -yy -q
sudo apt-get upgrade -yy -q

sleep 10

sudo apt-get install -yy -q gcc make unattended-upgrades curl nginx git unzip build-essential apache2-utils lxc wget bsdtar python-pip openjdk-8-jdk htop rpl

# timezone and ntp
sudo timedatectl set-timezone UTC
sudo timedatectl set-ntp on

echo '>>> AWS CLI'
cd /opt
sudo curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
sudo unzip awscli-bundle.zip
sudo ./awscli-bundle/install -i /opt/aws
sudo rm awscli-bundle.zip
sudo rm -r awscli-bundle

# so datadog can read it, if we need it to.
echo '>>> nginx log file perms'
sudo chmod 644 /var/log/nginx/*.log
