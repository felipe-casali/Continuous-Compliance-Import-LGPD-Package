#!/bin/bash
#
# This script will create BR LGPD Profiles from CSV file
#

source apiHostInfo

AUTH_HEADER=`cat Authorization`
count=0

> lista_prof_exp_ids.txt

while IFS=$'\t' read domainName expressionName regularExpression dataLevelProfiling
do
        [ "$domainName" == '"domainName"' ] && continue                       # Skip the Header
        #echo $domainName
        #echo $expressionName
        #echo $regularExpression
        #echo $dataLevelProfiling    # Print values read in variable
        curl -X POST --output profile_expression_id.txt --header ''"$AUTH_HEADER"'' --header 'Content-Type: application/json' --header 'Accept: application/json' --data "{\"domainName\": $domainName, \"expressionName\": $expressionName, \"regularExpression\": $regularExpression , \"dataLevelProfiling\": $dataLevelProfiling}" "http://$MASKING_ENGINE/masking/api/profile-expressions"
        rc=$?
        if [ $rc -eq 0 ]
        then
            cat profile_expression_id.txt | grep -v "Profile" | awk -F':' '{ print $2 }' | awk -F',' '{print $1}' >> lista_prof_exp_ids.txt
        fi
done < temp_profile_expressions.csv

exit 0