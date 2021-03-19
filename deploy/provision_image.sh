#!/bin/bash

echo "Provisioning a GCP virtual machine with ansible"
echo
read -p "Ook image name (e.g. from pack_image.sh): " -e source_image
read -p "Server name: " -e -i "ook-staging-$(date -u '+%Y%m%d-%H%M')" server_name
echo

cd ansible

env ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook server.yml \
  --extra-vars "profile=ook_staging \
                service_account_file=$GCLOUD_ACCOUNT_FILE \
                source_image=$source_image \
                server_name=$server_name"

cd ..
