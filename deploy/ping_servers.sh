#!/bin/bash

set -e

echo "Pinging GCP servers..."

env GCP_SERVICE_ACCOUNT_FILE=$GCLOUD_ACCOUNT_FILE ansible "*" -i ansible/gcp.yml -m ansible.builtin.ping