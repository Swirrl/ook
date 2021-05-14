#!/bin/bash

echo "Building a GCP image for ook with Packer"
echo
read -p "Base image name: " -e -i "ook-base-1620388107" base_image_name
read -p "Ook omni-package version (e.g. from https://app.circleci.com/pipelines/github/Swirrl/ook): " -e ook_package_version
echo

cd packer

packer build \
  -var gcloud_project=ons-pilot \
  -var base_image_name=$base_image_name \
  -var template_name=ook-staging \
  -var ook_package_name=ook \
  -var ook_package_version=$ook_package_version \
  -var gcloud_account_file=$GCLOUD_ACCOUNT_FILE \
  -var deploy_s3_access_key=$AWS_ACCESS_KEY_ID \
  -var deploy_s3_secret_key=$AWS_SECRET_ACCESS_KEY \
  template.json

cd ..
