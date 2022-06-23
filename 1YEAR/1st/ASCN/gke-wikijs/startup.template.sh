#!/bin/bash

# Set the gcloud project name
gcloud config set project ${GCP_PROJECT_NAME}

# Activate the gcloud service account
gcloud auth activate-service-account --key-file=${SERVICE_ACCOUNT_PRIVATE_KEY_JSON}
