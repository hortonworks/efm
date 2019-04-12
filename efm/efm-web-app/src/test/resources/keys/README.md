<!--
   - (c) 2018-2019 Cloudera, Inc. All rights reserved.
   -
   -  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
   -  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
   -  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
   -  properly licensed third party, you do not have any rights to this code.
   -
   -  If this code is provided to you under the terms of the AGPLv3:
   -   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
   -   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
   -       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
   -   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
   -       FROM OR RELATED TO THE CODE; AND
   -   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
   -       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
   -       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
   -       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
   - 
   - This file incorporates works covered by the following copyright and permission notice:
   -
   -    Apache NiFi - MiNiFi
   -    Copyright 2017-2018 The Apache Software Foundation
   -
   -    Licensed to the Apache Software Foundation (ASF) under one or more
   -    contributor license agreements.  See the NOTICE file distributed with
   -    this work for additional information regarding copyright ownership.
   -    The ASF licenses this file to You under the Apache License, Version 2.0
   -    (the "License"); you may not use this file except in compliance with
   -    the License.  You may obtain a copy of the License at
   -      http://www.apache.org/licenses/LICENSE-2.0
   -    Unless required by applicable law or agreed to in writing, software
   -    distributed under the License is distributed on an "AS IS" BASIS,
   -    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   -    See the License for the specific language governing permissions and
   -    limitations under the License.
-->
# Test Keys

The automated security tests require keys and certificates for TLS connections. 
The keys in this directory can be used for that purpose.

***

**NOTICE**: This directory contains keys and certificates for *development and testing* purposes only.

**Never use these keystores and truststores in a real-world scenario where actual security is needed.** 

The CA and private keys have been published in plaintext on the Internet, so they should never be trusted.

***  

## Directory Contents

### Certificate Authority (CA)

| Hostname / DN | File | Description | Format | Password |
| --- | --- | --- | --- | --- |
| - | ca-cert.pem | CA public cert | PEM (unencrypted) | N/A |
| - | ca-key.pem | CA private (signing) key | PEM | password |
| - | ca-ts.jks | CA cert truststore (shared by clients and servers) | JKS | password |
| - | ca-ts.p12 | CA cert truststore (shared by clients and servers) | PKCS12 | password |
| c2, localhost | c2-cert.pem | C2 server public cert | PEM (unencrypted) | N/A |
| c2, localhost | c2-key.pem | C2 server private key | PEM | password |
| c2, localhost | c2-ks.jks | C2 server key/cert keystore | JKS | password |
| c2, localhost | c2-ks.p12 | C2 server key/cert keystore | PKCS12 | password |
| proxy, localhost | proxy-cert.pem | Proxy server public cert | PEM (unencrypted) | N/A |
| proxy, localhost | proxy-key.pem | Proxy server private key | PEM | password |
| proxy, localhost | proxy-ks.jks | Proxy server key/cert keystore | JKS | password |
| proxy, localhost | proxy-ks.p12 | Proxy server key/cert keystore | PKCS12 | password |
| CN=admin, O=Cloudera | admin-cert.pem | client (user="admin") public cert | PEM (unencrypted) | N/A |
| CN=admin, O=Cloudera | admin-key.pem | client (user="admin") private key | PEM | password |
| CN=admin, O=Cloudera | admin-ks.jks | client (user="admin") key/cert keystore | JKS | password |
| CN=admin, O=Cloudera | admin-ks.p12 | client (user="admin") key/cert keystore | PKCS12 | password |
| CN=agent, O=Cloudera | agent-cert.pem | client (user="agent") public cert | PEM (unencrypted) | N/A |
| CN=agent, O=Cloudera | agent-key.pem | client (user="agent") private key | PEM | password |
| CN=agent, O=Cloudera | agent-ks.jks | client (user="agent") key/cert keystore | JKS | password |
| CN=agent, O=Cloudera | agent-ks.p12 | client (user="agent") key/cert keystore | PKCS12 | password |
| CN=alice, O=Cloudera | alice-cert.pem | client (user="alice") public cert | PEM (unencrypted) | N/A |
| CN=alice, O=Cloudera | alice-key.pem | client (user="alice") private key | PEM | password |
| CN=alice, O=Cloudera | alice-ks.jks | client (user="alice") key/cert keystore | JKS | password |
| CN=alice, O=Cloudera | alice-ks.p12 | client (user="alice") key/cert keystore | PKCS12 | password |

## Example Use with Curl

1. Configure EFM for TLS and Proxy Authentication with DN Whitelist

       efm.web.host=localhost
       
       efm.security.tls.enabled=true
       efm.security.tls.keystore=/path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/c2-ks.jks
       efm.security.tls.keystoreType=jks
       efm.security.tls.keystorePasswd=password
       efm.security.tls.keyPasswd=
       efm.security.tls.truststore=/path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-ts.jks
       efm.security.tls.truststoreType=jks
       efm.security.tls.truststorePasswd=password
           
       efm.security.user.proxy.enabled=true
       efm.security.user.proxy.headerName=x-webauth-user
       efm.security.user.proxy.dnWhitelist[0]=CN=proxy, O=Cloudera

2. Start the C2 server

       /path/to/minifi-c2/efm/efm-assembly/target/efm-*-bin/efm-*/bin/efm.sh run

3. Try the following curl calls to the C2 server (server must be accessed with the hostname 'localhost' or 'c2' for TLS hostname verification to work):

       # A request without the proxy auth header is authenticated using the client cert:
       curl -v \
         --cacert /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-cert.pem \
         --key /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/agent-key.pem \
         --pass password \
         --cert /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/agent-cert.pem \
         https://localhost:10080/efm/api/access
       
       # A request with the proxy auth header (and a whitelisted client cert) is authenticated using the specified user:
       curl -v \
         --cacert /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-cert.pem \
         --key /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/proxy-key.pem \
         --pass password \
         --cert /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/proxy-cert.pem \
         -H "X-WEBAUTH-USER: alice" \
         https://localhost:10080/efm/api/access

## Generating Additional Test Keys/Certs

If we need to add a service or user to our test environment that requires a cert signed by the same CA, 
here are the steps for generating additional keys signed by the same CA key to add to this directory.

Requirements:

- docker
- keytool (included with Java)
- openssl (included/available on most platforms)

If you do not have docker, you can substitute the nifi-toolkit binary, which is available for download from https://nifi.apache.org and should run on any platform with Java 1.8. 

### New Service Keys

The steps for generating a new service key/cert pair are (using `registry` as the example service):

```
# make working directory
rm -rf /tmp/test-keys && mkdir /tmp/test-keys && cd /tmp/test-keys

# copy existing CA key/cert pair to working directory, rename to default tls-toolkit names
cp /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-key.pem ./nifi-key.key
cp /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-cert.pem ./nifi-cert.pem

# use NiFi Toolkit Docker image to generate new keys/certs
docker run -v /tmp/test-keys:/tmp -w /tmp apache/nifi-toolkit:latest tls-toolkit standalone \
      --hostnames registry \
      --subjectAlternativeNames localhost \
      --nifiDnSuffix ", O=Cloudera" \
      --keyStorePassword password \
      --trustStorePassword password \
      --days 9999 \
      -O

# switch to output directory, create final output directory
cd /tmp/test-keys && mkdir keys

# copy new service key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore registry/keystore.jks -srcstoretype jks -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/registry-ks.jks -deststoretype jks -deststorepass password -destalias registry-key
keytool -importkeystore \
      -srckeystore keys/registry-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/registry-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/registry-ks.p12 -passin pass:password -out keys/registry-key.pem -passout pass:password
openssl pkcs12 -in keys/registry-ks.p12 -passin pass:password -out keys/registry-cert.pem -nokeys

echo "\nNew keys written to: /tmp/test-keys/keys"
```

You can verify the contents of the new keystore (and the CA used) using the following command:

    keytool -list -v -keystore /tmp/test-keys/keys/registry-ks.jks -storepass password

If you are satisfied with the results, you can copy the files from `/tmp/test-keys/keys` to this directory:
 
    cp /tmp/test-keys/keys/* /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/

### New Client or User Keys

The steps for generating a new user key/cert pair are (using `bob` as the example user):

```
# make working directory
rm -rf /tmp/test-keys && mkdir /tmp/test-keys && cd /tmp/test-keys

# copy existing CA key/cert pair to working directory, rename to default tls-toolkit names
cp /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-key.pem ./nifi-key.key
cp /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/ca-cert.pem ./nifi-cert.pem

# use NiFi Toolkit Docker image to generate new keys/certs
docker run -v /tmp/test-keys:/tmp -w /tmp apache/nifi-toolkit:latest tls-toolkit standalone \
      --clientCertDn "CN=bob, O=Cloudera" \
      --clientCertPassword password \
      --days 9999 \
      -O

# switch to output directory, create final output directory
cd /tmp/test-keys && mkdir keys

# copy new service key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore CN=bob_O=Cloudera.p12 -srcstoretype PKCS12 -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/bob-ks.jks -deststoretype JKS -deststorepass password -destalias bob-key
keytool -importkeystore \
      -srckeystore keys/bob-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/bob-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/bob-ks.p12 -passin pass:password -out keys/bob-key.pem -passout pass:password
openssl pkcs12 -in keys/bob-ks.p12 -passin pass:password -out keys/bob-cert.pem -nokeys

echo "\nNew keys written to: /tmp/test-keys/keys"
```

You can verify the contents of the new keystore (and that the signature is done by the correct CA) using the following command:

    keytool -list -v -keystore /tmp/test-keys/keys/bob-ks.jks -storepass password

If you are satisfied with the results, you can copy the files from `/tmp/test-keys/keys` to this directory:
 
    cp /tmp/test-keys/keys/* /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/


## Regenerating All Test Keys/Certs

In case you need to regenerate this entire directory, here are the steps that were used to first create it. 
Follow these steps in order to recreate it.

Requirements:

- docker
- keytool (included with Java)
- openssl (included/available on most platforms)

If you do not have docker, you can substitute the nifi-toolkit binary, which is available for download from https://nifi.apache.org and should run on any platform with Java 1.8. 

The steps for regenerating these test keys are:

```
# make working directory
rm -rf /tmp/test-keys && mkdir /tmp/test-keys && cd /tmp/test-keys

# use NiFi Toolkit Docker image to generate new CA and keys/certs
docker run -v /tmp/test-keys:/tmp -w /tmp apache/nifi-toolkit:latest tls-toolkit standalone \
      --certificateAuthorityHostname "C2 Test CA" \
      --hostnames c2,proxy \
      --subjectAlternativeNames localhost \
      --nifiDnSuffix ", O=Cloudera" \
      --keyStorePassword password \
      --trustStorePassword password \
      --clientCertDn "CN=agent, O=Cloudera" \
      --clientCertDn "CN=admin, O=Cloudera" \
      --clientCertDn "CN=alice, O=Cloudera" \
      --clientCertPassword password \
      --days 9999 \
      -O

# switch to output directory, create final output directory
cd /tmp/test-keys && mkdir keys

# copy CA key/cert to final output dir in all formats
cp nifi-key.key keys/ca-key.pem
cp nifi-cert.pem keys/ca-cert.pem
keytool -importkeystore \
      -srckeystore c2/truststore.jks -srcstoretype jks -srcstorepass password -srcalias nifi-cert \
      -destkeystore keys/ca-ts.jks -deststoretype jks -deststorepass password -destalias ca-cert
keytool -importkeystore \
      -srckeystore keys/ca-ts.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/ca-ts.p12 -deststoretype pkcs12 -deststorepass password

# copy c2 service key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore c2/keystore.jks -srcstoretype jks -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/c2-ks.jks -deststoretype jks -deststorepass password -destalias c2-key
keytool -importkeystore \
      -srckeystore keys/c2-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/c2-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/c2-ks.p12 -passin pass:password -out keys/c2-key.pem -passout pass:password
openssl pkcs12 -in keys/c2-ks.p12 -passin pass:password -out keys/c2-cert.pem -nokeys

# copy proxy service key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore proxy/keystore.jks -srcstoretype jks -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/proxy-ks.jks -deststoretype jks -deststorepass password -destalias proxy-key
keytool -importkeystore \
      -srckeystore keys/proxy-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/proxy-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/proxy-ks.p12 -passin pass:password -out keys/proxy-key.pem -passout pass:password
openssl pkcs12 -in keys/proxy-ks.p12 -passin pass:password -out keys/proxy-cert.pem -nokeys

# copy admin client key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore CN=admin_O=Cloudera.p12 -srcstoretype PKCS12 -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/admin-ks.jks -deststoretype JKS -deststorepass password -destkeypass password -destalias admin-key
keytool -importkeystore \
      -srckeystore keys/admin-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/admin-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/admin-ks.p12 -passin pass:password -out keys/admin-key.pem -passout pass:password
openssl pkcs12 -in keys/admin-ks.p12 -passin pass:password -out keys/admin-cert.pem -nokeys

# copy agent client key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore CN=agent_O=Cloudera.p12 -srcstoretype PKCS12 -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/agent-ks.jks -deststoretype JKS -deststorepass password -destkeypass password -destalias agent-key
keytool -importkeystore \
      -srckeystore keys/agent-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/agent-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/agent-ks.p12 -passin pass:password -out keys/agent-key.pem -passout pass:password
openssl pkcs12 -in keys/agent-ks.p12 -passin pass:password -out keys/agent-cert.pem -nokeys

# copy alice client key/cert to final output dir in all formats
keytool -importkeystore \
      -srckeystore CN=alice_O=Cloudera.p12 -srcstoretype PKCS12 -srcstorepass password -srcalias nifi-key \
      -destkeystore keys/alice-ks.jks -deststoretype JKS -deststorepass password -destkeypass password -destalias alice-key  
keytool -importkeystore \
      -srckeystore keys/alice-ks.jks -srcstoretype jks -srcstorepass password \
      -destkeystore keys/alice-ks.p12 -deststoretype pkcs12 -deststorepass password
openssl pkcs12 -in keys/alice-ks.p12 -passin pass:password -out keys/alice-key.pem -passout pass:password
openssl pkcs12 -in keys/alice-ks.p12 -passin pass:password -out keys/alice-cert.pem -nokeys

echo "\nKeys written to: /tmp/test-keys/keys"
```

You should now have a `/tmp/test-keys/keys` directory with all the necessary keys for testing with various tools.

You can verify the contents of a keystore using the following command:

    keytool -list -v -keystore "/tmp/test-keys/keys/proxy-ks.jks" -storepass password
    
If you are satisfied with the results, you can copy the files from `/tmp/test-keys/keys` to this directory:

    cp -f /tmp/test-keys/keys/* /path/to/minifi-c2/efm/efm-web-app/src/test/resources/keys/
