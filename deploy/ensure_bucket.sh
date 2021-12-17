#!/bin/bash

set -e

echo
echo "Ensuring Google Cloud Storage bucket is present for snapshot repository..."

env GCP_SERVICE_ACCOUNT_FILE=$GCLOUD_ACCOUNT_FILE ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook -v ansible/bucket.yml -i ansible/production-gcp.yml \
    --extra-vars "profile=ook_production \
                  es_snapshot_account_file=$ES_SNAPSHOT_ACCOUNT_FILE"
