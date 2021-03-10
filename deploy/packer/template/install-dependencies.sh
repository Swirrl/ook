#!/bin/bash
set -e

export AWS_ACCESS_KEY_ID=$DEPLOY_S3_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$DEPLOY_S3_SECRET_ACCESS_KEY
export GCLOUD_PROJECT
export KMS_LOCATION
export KMS_KEYRING
export KMS_KEY

echo ">>> adding user"
sudo adduser --system --home=/opt/ook ook

echo '>>> setting ownership'
sudo chown -R ook /opt/ook/

echo ">>> installing server"
sudo -E java -jar $OMNI_JAR install $OOK_PACKAGE_NAME -c $BOOTSTRAP_DIR/config/config.edn --environment prod --transitive
