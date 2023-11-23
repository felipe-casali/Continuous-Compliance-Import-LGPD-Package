#!/usr/bin/env bash

# Author: Felipe Casali / Solutions Enginner
# Contributors: Gustavo Gianini / Daniel Stolf

# Clear all credentials and IP addresses
clean_secrets(){
    > temp_profile_expressions.csv
    > apiHostInfo
    > loginCredentials.json
}

# Generate a temp profile expressions csv with the mandatory additional escapes
cat profile_expressions.csv | sed 's/\\/\\\\\\\\/g' > temp_profile_expressions.csv

echo -e "Before starting to import the LGPD package, let's connect to your Delphix Continuous Data engine:\n"

# Ask for the credentials for the masking engine
echo -n "IP address: "
read -r delphix_engine

echo -n "Username:"
read -r username

echo -n "Password: "
read -s password

# Validate if any input field is left blank. If an input field is left blank, display appropriate message and stop execution of script 
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

./Delphix_2022-09-12_Toolkit_Masking_Devkit_1.11.0/sdkTools/bin/maskScript install -j ./BR.jar  -H $delphix_engine -u $username
rc=$?

    if [ $rc -eq 0 ]
    then

        echo -e "\n > Algoritmos importados com sucesso."
        read -p "Pressione <ENTER> para continuar o import: ALGORITHMS, DOMAINS, PROFILES e PROFILESET."

        ./00_login.sh

        echo -e "\n > Creating Algorithms..."
        ./01_create_br_algorithms.sh

        echo -e "\n > Creating Domains..."
        ./02_create_br_domains.sh

        echo -e "\n > Creating Profiles..."
        ./03_create_br_profile.sh

        echo -e "\n > Creating ProfileSet..."
        ./04_create_br_profileset.sh

        echo -e "\n > Creating Classifiers..."
        ./05_create_br_classifiers.sh
        
        echo -e "\n > Creating Type Expressions..."
        ./06_create_br_typeexpressions.sh

        echo -e "\n > Congratulations. LGPD package imported with success."
        read -p "Press enter to finish."
    else
        echo -e "\n > Import failed. Please contact Delphix."
        read -p "Press enter to finish."
        clean_secrets
        exit 1
    fi

clean_secrets