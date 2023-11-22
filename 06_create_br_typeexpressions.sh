#!/bin/bash
set -euo pipefail
#
# This script will create BR LGPD Profile Type Expressions
#
# Author: Felipe Casali

source apiHostInfo
AUTH_HEADER=`cat Authorization`

while IFS=$'\t' read domainName typeExpressionName dataType minDataLength
do
        [ "$domainName" == '"domainName"' ] && continue                       # Skip the Header
        #echo $domainName
        #echo $expressionName
        #echo $regularExpression
        #echo $dataLevelProfiling    # Print values read in variable
        curl -X POST --output type_expression_id.txt --header ''"$AUTH_HEADER"'' --header 'Content-Type: application/json' --header 'Accept: application/json' --data "{\"domainName\": $domainName, \"typeExpressionName\": $typeExpressionName, \"dataType\": $dataType , \"minDataLength\": $minDataLength}" "http://$MASKING_ENGINE/masking/api/profile-type-expressions"
        rc=$?
        if [ $rc -eq 0 ]
        then
            cat type_expression_id.txt | grep -v "Profile" | awk -F':' '{ print $2 }' | awk -F',' '{print $1}' >> lista_type_exp_ids.txt
        fi
done < type_expression.csv

exit 0