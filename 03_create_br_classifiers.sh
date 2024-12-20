#!/bin/bash
set -x
#set -euo pipefail

# This script will create BR LGPD Profile Classifiers from pre-formatted JSON (CORRECTED)
# Author: Felipe Casali

source apiHostInfo
AUTH_HEADER=$(cat Authorization)

JSON_FILE="./JSON/classifiers.json"

# Iterate over each object in the JSON array (compact output)
jq -c '.[]' "$JSON_FILE" | while read -r PAYLOAD; do
    echo "Payload: $PAYLOAD"

    # Make the API request (using jq to extract the ID)
    CLASSIFIER_ID=$(curl -X POST  \
        --header "$AUTH_HEADER" \
        --header "Content-Type: application/json" \
        --header "Accept: application/json" \
        --data "$PAYLOAD" \
        "http://$MASKING_ENGINE/masking/api/v5.1.40/classifiers" | jq -r '.classifierId')

    rc=$?
    if [ $rc -eq 0 ]; then
        if [[ -n "$CLASSIFIER_ID" ]]; then
            echo "$CLASSIFIER_ID" >> lista_classifiers_ids.txt
        else
            echo "Warning: ID not found in response for payload: $PAYLOAD" >&2
        fi
    else
        echo "Error: Failed to create classifier for payload: $PAYLOAD" >&2
    fi
done
exit 0