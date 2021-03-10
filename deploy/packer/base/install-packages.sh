#!/bin/bash

set -e #exit on error

echo '>> INSTALL PACKAGES'

# install basics

sudo apt update -y -q
sudo apt upgrade -y -q

sudo apt install -y -q gcc make unattended-upgrades curl nginx git unzip build-essential apache2-utils lxc wget libarchive-tools openjdk-11-jdk htop rpl awscli

# timezone and ntp
sudo timedatectl set-timezone UTC
sudo timedatectl set-ntp on

# so datadog can read it, if we need it to.
echo '>>> nginx log file perms'
sudo chmod 644 /var/log/nginx/*.log




echo '>>> install elasticsearch'

# install elasticsearch open-source
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-oss-7.10.2-amd64.deb

sudo dpkg -i elasticsearch-oss-7.10.2-amd64.deb

# add opendistro for elasticsearch plug-ins
# https://opendistro.github.io/for-elasticsearch-docs/docs/install/deb/

#wget -qO - https://d3g5vo6xdbdb9a.cloudfront.net/GPG-KEY-opendistroforelasticsearch | sudo apt-key add -

#echo "deb https://d3g5vo6xdbdb9a.cloudfront.net/apt stable main" | sudo tee -a   /etc/apt/sources.list.d/opendistroforelasticsearch.list

#sudo apt-get update -y -q

#sudo apt install opendistroforelasticsearch -y -q
