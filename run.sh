#!/bin/sh

ENV=.conductr
python3 -m venv $ENV
. $ENV/bin/activate
pip3 install conductr-cli
