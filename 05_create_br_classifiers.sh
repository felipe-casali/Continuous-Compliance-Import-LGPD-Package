#!/bin/bash
set -euo pipefail
#
# This script will create BR LGPD Profile Classifiers
#
# Author: Felipe Casali

source apiHostInfo
AUTH_HEADER=`cat Authorization`

while IFS=$'\t' read domainName classifierName dataLevelProfiling matchStrength regex
do
        [ "$domainName" == '"domainName"' ] && continue                       # Skip the Header
        #echo $domainName
        #echo $expressionName
        #echo $regularExpression
        #echo $dataLevelProfiling    # Print values read in variable
        curl -X POST --output classifier_id.txt --header ''"$AUTH_HEADER"'' --header 'Content-Type: application/json' --header 'Accept: application/json' 
        --data "{\"domainName\": $domainName, \"classifierName\": $classifierName, \"dataLevelProfiling\": $dataLevelProfiling , \"matchStrength\": $matchStrength, \"regex\": $regex}" "http://$MASKING_ENGINE/masking/api/classifiers"
        rc=$?
        if [ $rc -eq 0 ]
        then
            cat classifier_id.txt | grep -v "Profile" | awk -F':' '{ print $2 }' | awk -F',' '{print $1}' >> lista_prof_exp_ids.txt
        fi
done < classifiers.csv

exit 0