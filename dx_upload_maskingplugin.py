#================================================================================
# File:         dx_upload_plugin.py
# Type:         python script
# Date:         Febuary 2nd 2021
# Author:       Carlos Cuellar
# Ownership:    This script is owned and maintained by the user, not by Delphix
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Copyright (c) 2019 by Delphix. All rights reserved.
#
# Description:
#
#       Script to be used to run a parent dsource/vdb sync process(to create a snapshot with latest changes) and then refresh a vDB
#
# Prerequisites:
#   Python 2/3 installed
#
#
# Usage
#   python dx_upload_plugin.py.py <DELPHIX USER> <DELPHIX PASSWORD> <DELPHIX_ENGINE> <PLUGIN>
#
# Example
#
#   python dx_upload_plugin.py admin delphix carlos606.dlpxdc.co artifact.json
#
#================================================================================
#
import sys
import requests
import json
import time
import socket
a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
import os
import logging
import subprocess
import binascii
import os
import gzip
import struct
from requests import ReadTimeout, ConnectTimeout, HTTPError, Timeout, ConnectionError
from requests_toolbelt.multipart.encoder import MultipartEncoder
from requests_toolbelt.multipart import encoder

DMUSER=sys.argv[1]
DMPASS=sys.argv[2]
DX_ENGINE=sys.argv[3]
DX_PLUGINFILE=sys.argv[4]
#
print ("Starting script...")
print ('------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------')
print ("Connecting to " + DX_ENGINE)
#
DX_ENGINE_PORT = (DX_ENGINE, 80)
try:
  result_of_check = a_socket.connect_ex(DX_ENGINE_PORT)
except:
  sys.exit("Please review the Delphix Engine IP/DNS value! Exiting now...")

#
#
#

#

print ('Using credentials to authenticate...')
if result_of_check == 0:
    print("Port 80 is open")
    BASEURL='http://' + DX_ENGINE + '/masking/api'
else:
    print("Port 80 is not open. Switching to 443.")
    BASEURL='https://' + DX_ENGINE + '/masking/api'
a_socket.close()
#
# Request Headers ...
#
req_headers = {
   'Content-Type': 'application/json'
}

#
# Python session, also handles the cookies ...
#
session = requests.session()
#
# Create session ...
#
#formdata = '{ "type": "APISession", "version": { "type": "APIVersion", "major": 1, "minor": 10, "micro": 0 } }'
#r = session.post(BASEURL+'/session', data=formdata, headers=req_headers, allow_redirects=False, verify=False)
#
# Login ...
#
formdata = '{ "username": "' + DMUSER + '", "password": "' + DMPASS + '" }'
print(BASEURL)
r = session.post(BASEURL+'/login', data=formdata, headers=req_headers, allow_redirects=False, verify=False)
#print(r.text)
authj = json.loads(r.text)
if "Error Message" in authj: 
    print ('Please provide correct user name and password! Error details: ' + str(authj))
    sys.exit(1)
else:
    print('Authentication successful!')

#
#  ...
#

#new code

url=BASEURL+"/file_uploads"
print(authj)
print(url)

payload={}
files=[
  ('file',('BR.jar',open('/Users/felipe.casali/Desktop/Continuous-Compliance-Import-LGPD-Package/BR.jar','rb'),'application/java-archive'))
]
headers = authj

response = requests.request("POST", url, headers=headers, data=payload, files=files)

print(response.text)

#new code 