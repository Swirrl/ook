#!/bin/bash

echo "Deploying an ook build to an existing server with ansible"
echo
read -p "Ook omni-package version (e.g. from https://app.circleci.com/pipelines/github/Swirrl/ook): " -e ook_package_version
echo

cd ansible

# https://docs.ansible.com/ansible/latest/collections/google/cloud/gcp_compute_inventory.html#parameter-service_account_file
export GCP_SERVICE_ACCOUNT_FILE=$GCLOUD_ACCOUNT_FILE

env ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook deploy.yml -i production-gcp.yml \
  --extra-vars "ook_package_version=$ook_package_version \
                aws_access_key_id=$AWS_ACCESS_KEY_ID \
                aws_secret_access_key=$AWS_SECRET_ACCESS_KEY"

cd ..
