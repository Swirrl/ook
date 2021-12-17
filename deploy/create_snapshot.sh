#!/bin/bash

set -e

echo
echo "Creating Elasticsearch Snapshot..."
echo
read -p "IP address of source server (check the ping-servers.sh script): " -e host
read -p "Snapshot name (defaults to datestamp): " -e -i "%3Csnapshot_%7Bnow%2Fd%7D%3E" snapshot_name
echo

env GCP_SERVICE_ACCOUNT_FILE=$GCLOUD_ACCOUNT_FILE ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook -v ansible/snapshot.yml -i "$host," \
      --extra-vars "snapshot_name=$snapshot_name"
