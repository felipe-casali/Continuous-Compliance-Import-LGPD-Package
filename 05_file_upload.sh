#!/bin/bash
#
# This script will upload a file to Delphix
#

source apiHostInfo
AUTH_HEADER=`cat Authorization`
count=0

MASKING_ENGINE=192.168.15.149
FILE=$PWD/SL_lists/SIGLAS_UF_BR_SL.txt

curl -X 'POST' \
  "http://$MASKING_ENGINE/masking/api/v5.1.20/file-uploads?permanent=false" \
  -H 'accept: application/json' \
  -H 'Authorization: a7ea23a3-0e42-4541-a3f4-de126e461081' \
  -H 'Content-Type: multipart/form-data' \
  -F "file=@$FILE;type=text/plain"