#!/bin/bash

set -o errexit

chown -R circleci ./.circleci/*
chmod +x ./.circleci/*.sh
