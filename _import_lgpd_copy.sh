#!/bin/bash

# Clear all credentials and IP addresses
clean_secrets(){
    > temp_profile_expressions.csv
    > apiHostInfo
    > loginCredentials.json
}

# Generate a temp profile expressions csv with the mandatory additional escapes
cat profile_expressions.csv | sed 's/\\/\\\\\\\\/g' > temp_profile_expressions.csv

# Ask for the credentials for the masking engine
echo -n "Digite o endereço IP ou Hostname da engine de Continuous Compliance: "
read -r delphix_engine
echo " "

echo -e "Este script foi desenvolvido para ser executado com a senha padrão do usuário admin."
echo -e "A senha do admin está com o valor padrão? (S/N)"
read -r autentica_padrao

if [ autentica_padrao =! "S" ] || [ autentica_padrao =! "s"]
then
    echo -n "Digite a senha do usuário admin da Engine de Continuous Compliance: "
    read - password
else

    username="admin"
    password="Admin-12"

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

    echo -e "\n > Nesta primeira etapa, vamos importar os algoritimos avançados de LGPD na engine $delphix_engine."
    read -p "A senha do usuário admin será solicitada novamente. Pressione <ENTER> para prosseguir."

    echo -e "\nIniciando o import do algoritmos do LGPD..."

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

        echo -e "\n > Congratulations. LGPD package imported with success."
        read -p "Press enter to finish."
    else
        echo -e "\n > Import failed. Please contact Delphix."
        read -p "Press enter to finish."
        clean_secrets
        exit 1
    fi
fi

clean_secrets