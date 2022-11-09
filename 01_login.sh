#!/bin/bash
#set -euo pipefail
#
# This file contains all the login information for the masking engine.
#

# Login credentials for the Masking Engine.
#USERNAME="admin"
#PASSWORD="Admin-12"

# Login into a masking engine
#curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d @loginCredentials.json 'http://uvo1shpwko1wyifpt4n.vm.cld.sr/masking/api/login'

source apiHostInfo

main()
{
    get_token
}

get_token() {
    local token=$(curl --silent -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d @loginCredentials.json "http://$MASKING_ENGINE/masking/api/login")
    echo "Authorization: `echo $token | grep -o '"Authorization":"[^"]*' | grep -o '[^"]*$'`" > Authorization
}

main

exit 0