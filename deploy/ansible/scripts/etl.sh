#!/bin/bash

set -e

sudo systemd-run --unit ook-indexer bash -c 'cd /opt/ook && java -Dlog4j.configurationFile=etl-log4j2.xml -cp "ook.jar:lib/*" -Xmx3g clojure.main -m ook.index'