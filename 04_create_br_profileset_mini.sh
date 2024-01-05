#!/bin/bash
set -euo pipefail
#
# This script will create BR LGPD MINI Profile Set
#

source apiHostInfo
AUTH_HEADER=`cat Authorization`

# TODO - Filtrar os ID's dos Profiles criados, e atualizar o json

# Create list of profiler expression IDs with comma
lista=`tr "\n" ","  < lista_prof_exp_ids_mini.txt | sed 's/,$//'`

echo $lista

curl --silent -X POST --header ''"$AUTH_HEADER"'' --header 'Content-Type: application/json' --header 'Accept: application/json' --data "{\"profileSetName\": \"LGPD_MINI\", \"profileExpressionIds\": [ $lista ]}" "http://$MASKING_ENGINE/masking/api/profile-sets"

exit 0