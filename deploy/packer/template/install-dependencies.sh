#!/bin/bash
set -e

export AWS_ACCESS_KEY_ID=$DEPLOY_S3_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$DEPLOY_S3_SECRET_ACCESS_KEY
export GCLOUD_PROJECT
export KMS_LOCATION
export KMS_KEYRING
export KMS_KEY

echo '>>> making dirs'
sudo mkdir -p /opt/elasticsearch
sudo mkdir -p /opt/ook

echo ">>> Adding users user..."
sudo adduser --system --home=/opt/elasticsearch elasticsearch
sudo adduser --system --home=/opt/ook ook

echo ">>> installing server and dependencies"
sudo -E java -jar $OMNI_JAR install $OOK_PACKAGE_NAME -p $TEMPLATE_NAME -c $BOOTSTRAP_DIR/config/config.edn --environment prod --transitive

echo '>>> setting ownership'
sudo chown -R elasticsearch /opt/elasticsearch/
sudo chown -R ook /opt/ook/
