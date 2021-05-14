#!/bin/bash

set -e

echo
echo "Pinging production servers..."

env GCP_SERVICE_ACCOUNT_FILE=$GCLOUD_ACCOUNT_FILE ansible "*" -i ansible/production-gcp.yml -m ansible.builtin.ping