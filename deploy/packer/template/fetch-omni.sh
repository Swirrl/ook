#!/bin/bash

set -e

export AWS_ACCESS_KEY_ID=$DEPLOY_S3_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$DEPLOY_S3_SECRET_ACCESS_KEY

aws s3 cp s3://swirrl-apps/omni-latest.jar $OMNI_JAR
