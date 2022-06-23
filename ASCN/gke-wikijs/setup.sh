#!/bin/bash

export GCP_PROJECT_NAME=parabolic-byte-331511
export SERVICE_ACCOUNT_PRIVATE_KEY_JSON="parabolic-byte-331511-60dc0a84c825.json"

envsubst \
    < ansible/playbook.template.yml \
    > ansible/playbook.yml

envsubst \
    < docker/startup.template.sh \
    > docker/startup.sh
