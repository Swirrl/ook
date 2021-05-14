#!/bin/bash

echo "Building a GCP base image with Packer"
echo

cd packer

packer build \
  -var gcloud_project=ons-pilot \
  -var gcloud_account_file=$GCLOUD_ACCOUNT_FILE \
  -var output_image_name=ook-base \
  base.json

cd ..
