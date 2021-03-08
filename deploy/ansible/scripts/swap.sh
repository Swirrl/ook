#!/bin/bash
set -e #exit on error
echo '>> SWAP'

## add 8G swap.
if [ -e /var/swap.1  ]; then
    echo '>>> WARNING: swap already exists. Not adding.'
else
    echo '>>> creating swap space: might take a minute...'
    sudo /bin/dd if=/dev/zero of=/var/swap.1 bs=1M count=8192
    sudo chown root:root /var/swap.1
    sudo chmod 600 /var/swap.1
    sudo /sbin/mkswap /var/swap.1
    sudo /sbin/swapon /var/swap.1
    echo "/var/swap.1 swap swap defaults 0 0" | sudo tee -a /etc/fstab
    sudo swapon -a
fi
