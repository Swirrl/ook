#!/bin/bash
set -e #exit on error

rm -rf /tmp/unpack-ook
mkdir /tmp/unpack-ook
aws s3 cp s3://swirrl-apps/packages/ook/$OOK_PACKAGE_VERSION/ook-$OOK_PACKAGE_VERSION.zip /tmp/unpack-ook/ook.zip
unzip /tmp/unpack-ook/ook.zip install/ook.zip -d /tmp/unpack-ook
unzip /tmp/unpack-ook/install/ook.zip ook.jar -d /tmp/unpack-ook/install
mv /tmp/unpack-ook/install/ook.jar /opt/ook/ook-$OOK_PACKAGE_VERSION.jar
rm -f /opt/ook/ook.jar
ln -s /opt/ook/ook-$OOK_PACKAGE_VERSION.jar /opt/ook/ook.jar
rm -rf /tmp/unpack-ook