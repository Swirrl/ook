#!/bin/bash
set -e #exit on error

echo '>> ELASTICSEARCH'

echo '>>> starting elasticsearch'
sudo systemctl start elasticsearch.service

wget -nv -O - --retry-connrefused --tries 30 --waitretry=2 localhost:9200 2>&1 > /dev/null | uniq

echo '>>> OOK'

echo '>>> making ook dirs'
# for logs
sudo mkdir -p /var/log/ook
sudo chown -R ook /var/log/ook

# TODO: move into ook package?
sudo ln -s /etc/systemd/system/ook.service /etc/systemd/system/multi-user.target.wants/ook.service

echo '>>> starting ook service'
sudo systemctl start ook

wget -nv -O - --retry-connrefused --tries 60 --waitretry=2 localhost:3010 2>&1 > /dev/null | uniq

echo '>>> NGINX'

echo '>>> restarting nginx'
sudo systemctl restart nginx
