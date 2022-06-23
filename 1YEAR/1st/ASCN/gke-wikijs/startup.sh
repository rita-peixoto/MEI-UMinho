#!/bin/bash

# Set the gcloud project name
gcloud config set project parabolic-byte-331511

# Activate the gcloud service account
gcloud auth activate-service-account --key-file=parabolic-byte-331511-60dc0a84c825.json
