#!/bin/bash
#
# This script will create BR LGPD Domains from CSV file
#

source apiHostInfo
AUTH_HEADER=`cat Authorization`
count=0

while IFS="," read -r rec_column1 rec_column2
do
    DOMAIN_NAME=`echo $rec_column1`
    ALG_NAME=`echo $rec_column2`
    echo $DOMAIN_NAME $ALG_NAME
    echo ""
    curl -X POST --header ''"$AUTH_HEADER"'' --header 'Content-Type: application/json' --header 'Accept: application/json' --data "{\"domainName\": $DOMAIN_NAME, \"defaultAlgorithmCode\": $ALG_NAME}" "http://$MASKING_ENGINE/masking/api/domains"
    echo " "
    count=$(( $count + 1 ))
done < <(tail -n +2 domains.csv)

echo "Created $count of $count Domains with success."
exit 0