#!/usr/bin/env bash

clean_secrets(){
    cat " " > temp_profile_expressions.csv
    cat " " > apiHostInfo
    cat " " > loginCredentials.json
}

cat profile_expressions.csv | sed 's/\\/\\\\\\\\/g' > temp_profile_expressions.csv

echo -e "Before starting to import the LGPD package, let's connect to your Delphix Continuous Data engine:\n"

echo -n "IP address: "
read -r delphix_engine

echo -n "Username:"
read -r username

echo -n "Password: "
read -r password

# Firstly Validate if any input field is left blank. If an input field is left blank, display appropriate message and stop execution of script 
if [ -z "$delphix_engine" ] || [ -z "$username" ] || [ -z "$password" ]
then 
    echo "\nInputs cannot be blank please try again!"
    read -p "Press enter to finish."
    exit 0
fi 

# Update loginCredentials.json
echo "{" > loginCredentials.json
echo " \"username\": \"$username\", " >> loginCredentials.json
echo " \"password\": \"$password\" " >> loginCredentials.json
echo "}" >> loginCredentials.json

# Update apiHostInfo
echo "# Delphix Engine" > apiHostInfo
echo "MASKING_ENGINE=$delphix_engine" >> apiHostInfo

echo -e "\n > In the first step LGPD Algorithms will be imported on $delphix_engine."
read -p "The Masking Engine password will be asked once more. Press enter to continue."

echo -e "\nStarting to import LGPD package..."

./Delphix_2022-09-12_Toolkit_Masking_Devkit_1.11.0/sdkTools/bin/maskScript install -j /Users/felipe.casali/Documents/dxtoolkit2/lgpd_scripts/BR.jar  -H $delphix_engine -u $username
rc=$?

if [ $rc -eq 0 ]
then

    echo -e "\n > LGPD Algorithms imported with success."
    read -p "Press enter to continue importing DOMAINS, PROFILES and PROFILESET"

    ./01_login.sh

    echo -e "\n > Creating Domains..."
    ./02_create_br_domains.sh

    echo -e "\n > Creating Profiles..."
    ./03_create_br_profile.sh

    echo -e "\n > Creating ProfileSet..."
    ./04_create_br_profileset.sh

    echo -e "\n > Congratulations. LGPD package imported with success."
    read -p "Press enter to finish."
else
    echo -e "\n > Import failed. Please contact Delphix."
    read -p "Press enter to finish."
    clean_secrets
    exit 1
fi

clean_secrets