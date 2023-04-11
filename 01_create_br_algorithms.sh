#!/bin/bash
#
# This script will create BR Algorithms from CSV file
#

source apiHostInfo
AUTH_HEADER=`cat Authorization`
count=0
MASKING_ENGINE=192.168.15.149

while IFS="," read -r rec_column1 rec_column2 rec_column3 rec_column4 rec_column5 rec_column6
do
    ALG_NAME="$rec_column1"
    FILE=$PWD$rec_column6
    curl -X POST --header "$AUTH_HEADER" --header 'Content-Type: application/json' --header 'Accept: application/json' --data "{\"algorithmName\": \"$ALG_NAME\", \"algorithmType\": \"COMPONENT\", \"description\": \"teste\", \"pluginId\": 7, \"frameworkId\": 3, \"algorithmExtension\": { \"lookupFile\": { \"uri\": \"file://$FILE\" } } }" "http://$MASKING_ENGINE/masking/api/algorithms"
    count=$(( count + 1 ))
done < <(tail -n +2 algorithms.csv)

echo "Created $count of $count Algorithms with success."
exit 0