#!/bin/bash
#
# This script will create BR Algorithms from CSV file
#

source apiHostInfo
AUTH_HEADER=`cat Authorization`
count=0

#Get the Framework ID of Secure Lookup Algorithm
curl --location "http://$MASKING_ENGINE/masking/api/algorithm/frameworks" --header "$AUTH_HEADER" -o response_o.json
frameworkId=`jq '.responseList[] | select(.frameworkName == "Secure Lookup") | .frameworkId' response_o.json`

while IFS="," read -r rec_column1 rec_column2 rec_column3 rec_column4 rec_column5 rec_column6
do
    ALG_NAME="$rec_column1"
    FILE=$PWD$rec_column6
    #Upload the text file and capture the fileReferenceId
    response=`curl --location "http://$MASKING_ENGINE/masking/api/file-uploads?permanent=false" --header "$AUTH_HEADER" --form file=@\"$FILE\"`
    fileReferenceId=$(echo "$response" | sed -n 's/.*"fileReferenceId":"\([^"]*\)".*/\1/p')
    echo $fileReferenceId
    echo -e "Pressione Enter para continuar"
    #Create the Algorithm
    curl -X POST --header "$AUTH_HEADER" --header 'Content-Type: application/json' --header 'Accept: application/json' --data "{\"algorithmName\": \"$ALG_NAME\", \"algorithmType\": \"COMPONENT\", \"description\": \"teste\", \"pluginId\": 7, \"frameworkId\": \"$frameworkId\", \"algorithmExtension\": { \"lookupFile\": { \"uri\": \"$fileReferenceId\" } } }" "http://$MASKING_ENGINE/masking/api/algorithms"
    count=$(( count + 1 ))
done < <(tail -n +2 algorithms.csv)

echo "Created $count of $count Algorithms with success."
exit 0