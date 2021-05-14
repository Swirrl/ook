#!/bin/bash

set -e

echo
echo "Starting ETL process on production servers..."

env GCP_SERVICE_ACCOUNT_FILE=$GCLOUD_ACCOUNT_FILE ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook -v ansible/etl.yml -i ansible/production-gcp.yml

echo "You can follow progress with the following command on the remote machine:"
echo
echo "    journalctl -f -u etl"
echo