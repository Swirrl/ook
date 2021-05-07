#!/bin/bash

set -e

export AWS_ACCESS_KEY_ID=$DEPLOY_S3_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$DEPLOY_S3_SECRET_ACCESS_KEY

sudo mkdir $(dirname $OMNI_JAR)
sudo mkdir /etc/opt/omni/
sudo mv /tmp/omni-config/ook.edn /etc/opt/omni/ook.edn

aws s3 cp s3://swirrl-apps/omni-latest.jar /tmp/omni.jar

sudo mv /tmp/omni.jar $OMNI_JAR