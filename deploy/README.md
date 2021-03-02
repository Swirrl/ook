# Deploying OOK

## Prerequisites

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
