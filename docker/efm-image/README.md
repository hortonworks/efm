# C2 Docker Image
A Dockerfile is provided for generating a Docker image off the most recent source.

To create a Docker image, a user can perform `make docker` from the root of the source checkout.

In the event that you do not have GNU make installed on your system, an image can be generated from the source checkout root via:
  1. Build a c2 assembly: `mvn package -DskipTests -f efm/pom.xml`
  2. Generate a tagged Docker image: `docker build -t hortonworks/minifi-c2:latest -f docker/efm-image/Dockerfile .`
    * Optionally, a build argument (--build-arg SRC_COMMIT_HASH=<commit hash>) can be provided to capture the commit hash of the code used to store this value in the environment variable, ${COMMIT_HASH}.

## Prerequisites
  * Docker Engine, 17.12.0+

## Configuration

Any c2.properties key can be specified to the Docker container using a corresponding environment variable.
In general, these are the c2.properties keys replacing periods (.) with underscores (\_) and using upper case.

| Function                                             | Property                   | Docker Environment Variable |
|------------------------------------------------------|----------------------------|-----------------------------|
| Binding Web Server Hostname or Address               | efm.server.address          | EFM_SERVER_ADDRESS           |
| Apache NiFi - Registry Address                       | efm.nifi.registry.url       | EFM_NIFI_REGISTRY_URL        |
| Apache NiFi - Registry Bucket to Use for Agent Flows | efm.nifi.registry.bucketId  | EFM_NIFI_REGISTY_BUCKETID    |


## LICENSE
Source is made available under the terms of the [GNU Affero General Public License (GNU AGPLv3)](https://www.gnu.org/licenses/agpl-3.0.en.html).

(c) 2018-2019 Cloudera, Inc. All rights reserved.

This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
properly licensed third party, you do not have any rights to this code.

If this code is provided to you under the terms of the AGPLv3:

(A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;

(B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
    LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;

(C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
    FROM OR RELATED TO THE CODE; AND

(D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
    DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
    TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
    UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
