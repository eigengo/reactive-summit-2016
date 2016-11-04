#!/bin/sh

# prepare fresh virtual environment
ENV=.conductr
rm -rf $ENV

# Ensure that venv is ready
python3 -m venv $ENV

# switch to this venv
. $ENV/bin/activate

# Install the conductor-cli tooling
pip3 install conductr-cli

# Load the cassandra bundle
# conduct load -v cassandra --api-version 1
