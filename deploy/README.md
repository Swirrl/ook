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

### Google Cloud SDK

1. Install the Google Cloud SDK](https://cloud.google.com/sdk/downloads#interactive).
   - It's also available as a [homebrew package](https://formulae.brew.sh/cask/google-cloud-sdk)
2. Ensure that the `gcloud` tool is on your `$PATH`.
3. Setup an auth token with `gcloud init`.

You'll need a [GCP Service Account](https://console.cloud.google.com/iam-admin/serviceaccounts) to build images and provision servers etc.

### Credentials

You'll need the following secrets (identified as environment variables in the instructions below):

- `$GCLOUD_ACCOUNT_FILE` - path to a json file providing authentication credentials for your CGP service account
- `$AWS_ACCESS_KEY_ID` and `$AWS_SECRET_ACCESS_KEY` - credentials used to access the omni package repository on s3. These are not stored in the image so you can use your personal creds.

An example for encrypting secrets and loading them automatically with [direnv](https://direnv.net/) is provided in [.envrc.example](./.envrc.example).

## 1. Building the images with packer

The production server will be provisioning from a disk image built by packer.

### 1.1 Build a base image

The base image include the OS and ES and shouldn't change often.

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
  -var gcloud_account_file=$GCLOUD_ACCOUNT_FILE \
  -var output_image_name=ook-base \
  base.json
```

You might like to update the base image name in [pack_image.sh](./pack-image.sh) (for the ook-specific image in the next step) if you change it.


### 1.2 Build an ook image

This extends the base image with a specific version of ook.

There's a script to automate this in [pack_image.sh](./pack-image.sh).
You'll need to provide an omni package version (e.g. from CI, see example below).

The remainder of this section explains the packer command.

```#
cd packer

packer build \
  -var gcloud_project=<project-name> \
  -var base_image_name=<base-image-name> \
  -var template_name=<template-name> \
  -var ook_package_name=<ook-package-name> \
  -var ook_package_version=<ook-package-version> \
  -var gcloud_account_file=<gcloud-account-json> \
  -var deploy_s3_access_key=<aws-access-key> \
  -var deploy_s3_secret_key=<aws-secret-key> \
  template.json
```

e.g. using a build from [CI](https://app.circleci.com/pipelines/github/Swirrl/ook):

```#
cd packer

packer build \
  -var gcloud_project=swirrl-staging-servers \
  -var base_image_name=ook-base-1614968110 \
  -var template_name=ook-staging \
  -var ook_package_name=ook \
  -var ook_package_version=km_package-and-deploy-circle_82_d74bc4 \
  -var gcloud_account_file=$GCLOUD_ACCOUNT_FILE \
  -var deploy_s3_access_key=$AWS_ACCESS_KEY_ID \
  -var deploy_s3_secret_key=$AWS_SECRET_ACCESS_KEY \
  template.json
```

Note the image name (used in the next step).

## 2. Deploying a server with ansible

We use ansible to provision the server.

There's a script to automate this in [deploy_image.sh](./deploy-image.sh).
You'll need to provide an image name (from the last step).
You can keep the default server name (unique by datetime).

The remainder of this section explains the ansible command.

```#
cd ansible

env ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook server.yml \
  --extra-vars "profile=<profile> \
                service_account_file=<gcloud-account-json> \
                source_image=<source-image> \
                server_name=<server-name>"
```

e.g
```#
cd ansible

env ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook server.yml \
  --extra-vars "profile=ook_staging \
                service_account_file=$GCLOUD_ACCOUNT_FILE \
                source_image=ook-staging-1614974526  \
                server_name=ook-staging-1599210947"
```
