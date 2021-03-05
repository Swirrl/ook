# Deploying OOK

## 0. Prerequisites

### Packer

Packer can be installed with brew:

    brew install packer

Alternatively you can download a binary from the [packer downloads page](https://www.packer.io/downloads.html)

This setup was developed using packer 1.7.0.

### Ansible

Follow the [installation guide](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html) to install ansible. This setup was developed with version 3.0.0 on python 3.7.6. In addition the GCE ansible module requires the `requests`, `google-auth` and `google-api-python-client` packages to be installed:

    python -m pip install --user requests google-auth google-api-python-client

## Google Cloud SDK

1. Install the Google Cloud SDK](https://cloud.google.com/sdk/downloads#interactive).
   - It's also available as a [homebrew package](https://formulae.brew.sh/cask/google-cloud-sdk)
2. Ensure that the `gcloud` tool is on your `$PATH`.
3. Setup an auth token with `gcloud init`.

## 1. Building the images with packer

### 1.1 Base image

This is only necessary if we want to change anything in the base image.

```#
cd packer
packer build -var gcloud_project=<project-name> \
  -var gcloud_account_file=<gcloud-account-json> \
  -var output_image_name=<base-image-name> \
  base.json
```

e.g.

```#
packer build -var gcloud_project=swirrl-staging-servers \
  -var gcloud_account_file=/Users/kmclean/code/swirrl/swirrl-staging-servers-db87289685aa.json \
  -var output_image_name=ook-base \
  base.json
```

### 1.2 Build project-specific image with packer

This includes a specific version of ook.

```#
cd packer

packer build \
  -var gcloud_project=<project-name> \
  -var base_image_name=<base-image-name> \
  -var template_name=<template-name> \
  -var ook_package_name=<ook-package-name> \
  -var ook_package_version=<ook-package-version> \
  -var gcloud_account_file=/path/to/gcloud-account-file \
  -var deploy_s3_access_key=<aws-access-key> \
  -var deploy_s3_secret_key=<aws-secret-key> \
  template.json
```

e.g.

```#
cd packer

packer build \
  -var gcloud_project=swirrl-staging-servers \
  -var base_image_name=ook-base-1614968110 \
  -var template_name=ook-staging \
  -var ook_package_name=ook \
  -var ook_package_version=km_package-and-deploy-circle_82_d74bc4 \
  -var gcloud_account_file=/Users/kmclean/code/swirrl/swirrl-staging-servers-db87289685aa.json \
  -var deploy_s3_access_key=$AWS_ACCESS_KEY_ID \
  -var deploy_s3_secret_key=$AWS_SECRET_ACCESS_KEY \
  template.json
```
```
