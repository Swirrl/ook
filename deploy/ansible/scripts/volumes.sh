#!/bin/bash
set -e #exit on error
echo '>> VOLUMES'

echo '>>> making dirs'
sudo mkdir -p /var/lib/elasticsearch

echo '>>> formatting volumes, if required...'

if sudo file -sL /dev/disk/by-id/google-elasticsearch_data | grep ext4
then
  echo ">>>> WARNING: elasticsearch disk already formatted ext4. Not formatting."
else
  echo '>>>> formatting /dev/disk/by-id/google-elasticsearch_data'
  sudo mkfs -t ext4 /dev/disk/by-id/google-elasticsearch_data
fi

echo '>>>> adding lines to fstab'
echo "/dev/disk/by-id/google-elasticsearch_data      /var/lib/elasticsearch     ext4 defaults,nofail 0 2" | sudo tee -a /etc/fstab

echo '>>>> running mount'
sudo mount -a
