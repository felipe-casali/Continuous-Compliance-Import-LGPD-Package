# Continuous-Compliance-Import-LGPD-Package
Import LGPD Algorithms, Domains, ProfileExpressions and ProfileSet

This project was developed to help and accelerate the import of LGPD data into your Delphix Continuous Compliance Engine (Masking)

This script can be executed on any linux server as long as you have installed the packages below:

- jq
- JRE 1.8

If you don't have an avaible Linux with the prerequisites above, the installation can be done directly on the Delphix Continuous Compliance VM.

Instructions:

1 - Login on the Delphix Continuous Compliance Engine via SSH using the delphix user. Or connect to anuy other Linux machine with the mentioned pre-reqs.

2 - Download a copy this project using the following command:

wget https://github.com/felipe-casali/Continuous-Compliance-Import-LGPD-Package/archive/refs/heads/main.zip

3 - Extract the content of the zip file running:

unzip main.zip

4 - Navigate to the Continuous-Compliance-Import-LGPD-Package-main folder:

cd Continuous-Compliance-Import-LGPD-Package-main

5 - Execute the ./import_lgpd.sh script and follow the instructions

NOTE THAT THE SCRIPT WILL ASK THE USER PASSWORD TWICE.

Watch a demo of this utility on https://github.com/felipe-casali/Continuous-Compliance-Import-LGPD-Package/blob/b3eff21fed1593e2c7717afd324e03ad0f8dac9a/Import_LGPD_Package.mp4
