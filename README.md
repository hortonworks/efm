# Cloudera Edge Management (CEM) - Edge Flow Manager (EFM)

## Prerequisites
* Java Development Kit 8
* Maven, 3+
* Docker (optional)
  * Docker Engine, 17.12.0+
  * Docker Compose, 3.5+

## Docker

**NOTE**
To utilize many of the base Docker images referenced in the [test environment](https://github.com/hortonworks/minifi-c2/tree/master/docker/test) of this project, you will need access to the [private, Hortonworks Docker Hub registry](https://cloud.docker.com/u/hortonworks/orgs/hortonworks/) as well as being a member of the [`nifi` team](https://cloud.docker.com/u/hortonworks/orgs/hortonworks/teams/nifi/users).  The organization and teams are maintained by the Hortonworks RE & QE teams.

### Docker Environment Quick Start Guide

Follow the instructions above to install dependencies and authenticate to the Cloudera Docker registry.

```
# start a test environment with an instance of every service
make env-up
```

It can take up to a minute for all services to be up and running. Once launched, you should be able to access the following services from the host machine:

| Service | Address | Description |
| --- | --- | --- |
| **Traefik Dashboard** | http://localhost:8080 | A dashboard that shows all proxy frontends and backends |
| **Web UI** | http://localhost/efm/ui | The web UI for the efm server |
| **Grafana** | http://localhost/grafana/d/prometheus-efm | efm Metrics dashboards |
| **NiFi** | http://localhost/nifi | A NiFi instance (a data destination for MiNiFi agents) |
| **NiFi Registry** | http://localhost/nifi-registry | A NiFi Registry instance shared by services |

Note that these are user-facing endpoints hosted by the proxy. Services do not use these endpoints for communicating to each other. For more information on server-to-service communication within the test environment, see the environment details section below.

```
# View logs from the services in the environment
make env-logs

# Scale the number of agents
make scale-agent AGENT_COUNT=3

# Tear down the environment (when done)
make env-down
```

### EFM Docker Image
A Dockerfile is provided for generating a Docker image off the most recent source.

To create a Docker image, a user can perform `make docker` from the root of the source checkout.

In the event that you do not have GNU make installed on your system, an image can be generated from the source checkout root via:
  1. Build a efm assembly: `mvn package -DskipTests -f efm/pom.xml`
  2. Generate a tagged Docker image: `docker build -t hortonworks/efm:latest -f docker/Dockerfile .`

### Docker Environment Details
The EFM Test Environment consists of test images extending base images available publicly or via other builds.  The images provided are:
  - EFM Server
  - Apache NiFi
  - Apache NiFi MiNiFi CPP Agent
  - Apache NiFi Registry
  - Apache NiFi Toolkit/CLI
  - Prometheus
  - Grafana
  - Traefik

In its current implementation, the environment will bring all services online and have the C++ Agents heartbeating to the efm Server.

The Makefile in the project serves as the primary driver for enabling all of these actions.  A typical sequence executed from the source root could be:
  1. `make env-up` - to start a test environment with an instance of every service
  2. `make scale-agent AGENT_COUNT=3` - to add an additional two agents for a total of 3 instances in the test environment
  3. `make env-logs` - to view logs for all services running in the test environment
  4. `make env-down` - to tear down the test environment and all associated Docker resources

Most services are accessed through the proxy service. Within the docker environment, there are two networks:

- private: all services that are part of the system, accessed through the proxy
- public: all other services (minifi efm agents)

The proxy service is a Traefik reverse proxy that sits on both networks (backends are on the private network, frontends are on the public network). The reverse proxy is accessible throgh two hostnames:

- `localhost` on port 80: the hostname that a user can use from the host machine in a Chrome browser
- `proxy on port` 80: the hostname that docker services can use to access the proxy on the docker networks

For example, after launching the environment, you can access services through the proxy from your host machine by going to http://localhost/path/to/service. But minifi agents would access services through the proxy by using http://proxy/path/to/service

## Makefile

### Targets
  * clean
    - Cleans the MiNiFi efm Maven project source
  * clean-compose-file
    - Cleans the generated docker-compose YAML file
  * clean-docker
    - Removes the test environment and all created Docker images
  * compose-file
    - Generates a merged docker-compose.yml file based on environment flags.
  * env-up
    - Creates the efm Test Environment via docker-compose
  * env-down
    - Destroys the efm Test Environment via docker-compose
  * env-logs _<svc=SERVICE_NAME> (optional)_
    - Provides the docker-compose logs of a running Test Environment
  * env-shell _<svc=SERVICE_NAME> (REQUIRED)_
    - Provides a shell of the specified service in the running Test Environment
  * efm/efm-assembly/target/efm-$(efm_version)-bin.tar.gz
    - Generates the efm assembly used primarily for the Docker image
  * efm-docker
    - Generates a efm Docker image, tagged `hortonworks/minifi-efm:latest`
  * minificpp-docker
    - Generates an Apache NiFi - MiNiFi C++ Docker image, tagged `hortonworks/test-nifi-minifi-cpp:latest`
  * test-nifi-docker
    - Generates an Apache NiFi Docker image, tagged `hortonworks/test-nifi:latest`
  * scale-agent AGENT_COUNT=##
    - Either scales an already running or starts a test environment with the number of instances specified

## IntelliJ Quick Development Setup

When using IntelliJ it is possible to run the efm application directly from IntelliJ with the ability to auto-reload changes.

1) In `"Preferences -> Build, Execution, & Deployment -> Compiler"` select `"Build Project Automatically"`

2) Open the IntelliJ Registry by pressing `"CMD + ALT + SHIFT + /"` and selecting Registry

3) Enable the option for `"compiler.automake.allow.when.app.running"`

4) Create a Run Configuration for efmApplication with the following configuration:

![IntelliJ Run Configuration for efmApplicaiton](docs/images/readme/intellij-efm-run-config.png)

The `"Program arguments"` should be:

    --spring.profiles.active=dev

The `"Working directory"` should be:

    $MODULE_WORKING_DIR$

5) Launch the application using the above configuration and then make a change to some code, the console should show the spring-boot application reloading.

NOTE: Sometimes the automatic build in IntelliJ seems to stop working. Restarting IntelliJ seems to resolve the issue, or forcing a build with  `"Build -> Build Project"` will work.

## Development Builds

The efm project is a web application with a separate frontend and backend. The backend is a Spring Boot based REST API web service. The frontend is an AngularJS webapp based on the NiFi Flow Design System (FDS).

A full build can be performed using:

    cd efm
    mvn clean install -Pcontrib-check

The resulting assembly can be run using:

    ./efm-assembly/target/efm-*-bin/efm-*/bin/efm.sh run

There are several optional build flags, including:

    -Pcontrib-check         # enable checkstyle checks
    -DskipTests             # disable all tests (unit and integration)
    -Ptest-all-dbs          # run integration tests against all supported databases (requires docker)
    -Pno-integration-tests  # disable Spring Boot integration tests (still run unit tests)
    -Dheadless              # build the backend without bundling the frontend (see below)

### Separate Frontend and Backend Builds

As development is usually decoupled between frontend work and backend work, it is usually faster to only recompile/build the part of the app being worked on at any given time.

To build and run a headless backend, use:

    cd efm
    mvn install -Dheadless -DskipTests
    ./efm-assembly/target/efm-*-bin/efm-*/bin/efm.sh run

To build and run the frontend standalone, use:

    1. cd efm-web-frontend/src/main/
    2. npm install
    3a. npm start
    # frontend now hosted at http://127.0.0.1:8080, proxies the backend API
    # make changes in src/main, see changes instantly by refreshing in browser
    -- or --
    3b. npm run watch
    # frontend now hosted at http://127.0.0.1:8080, proxies the backend API
    # make changes in src/main, see changes automatically refreshed in the browser

## Database Configuration

The efm application is setup to be database agnostic. By default the applications use an embedded H2 database with file-based persistence, but can be configured to run against different databases.

The supported databases other than H2 are currently MySQL and PostgreSQL. Database drivers for these databases are included with the assembly of the efm applcation. Other databases may also work, but require adding additional JDBC drivers to the application classpath and may schema SQL modification.

### Postgres Setup

The below steps are an example of configuring the efm application to run against Postgres.

1) Install Postgres (9.6 is regularly tested) and create a database for the application:

       createdb efm

2) Login to the Postgres shell and create a user with privileges to the above db:

       psql efm
       CREATE USER efm WITH PASSWORD 'efm';
       GRANT ALL PRIVILEGES ON DATABASE efm to efm;
       \q

3) Configure the database properties in efm.properties:

       efm.db.url=jdbc:postgresql://localhost/efm
       efm.db.driver.class=org.postgresql.Driver
       efm.db.username=efm
       efm.db.password=efm

### MySQL Setup

1) Install MySQL (5.6 and 5.7 are regularly tested)

2) Login the the MySQL shell and create a database and user for the application:

       CREATE DATABASE efm;
       GRANT ALL PRIVILEGES ON efm.* TO 'efm'@'localhost' IDENTIFIED BY 'efmPassword';
       -- For remote users, use 'efm'@'efmipaddress' if the efm ip address is known or 'efm'@'%' for any remote host

3) Configure the database properties in efm.properties:

       efm.db.url=jdbc:mysql://localhost/efm
       efm.db.driver.class=com.mysql.cj.jdbc.Driver
       efm.db.username=efm
       efm.db.password=efmPassword

## Security Configuration

By default, the application runs in unsecured mode in which the web endpoints are accessible over HTTP on all network interfaces and clients are not authenticated.
In unsecured mode, all clients are anonymous and have full access to the application.

Limiting the network interfaces that the web server will bind to is configurable in `efm.properties`:

```
efm.web.host=localhost
```

There are multiple user authentication options available, including:

- TLS Mutual Auth (client certificates)
- Knox SSO Authentication
- Generic SSO Reverse Proxy Authentication

Configuring these authentication options is covered in detail in the following sections.

With regards to *authorization*, by default, all authenticated users have full access to the application.
You can optionally configure a user whitelist that will block access to any user not included on the whitelist.
See the Authorized Users Whitelist section below for details.   

### TLS Mutual Authentication

You can configure the server to authenticate users based on a client certificate provided for TLS mutual auth.
The server's TLS settings, including what certificates it will trust, are configured using the `
efm.security.tls.*` prefixed properties in the `efm.properties` file.

```
efm.server.ssl.enabled=false
efm.server.ssl.keyStore=/path/to/keystore.jks
efm.server.ssl.keyStoreType=jks
efm.server.ssl.keyStorePassword=
efm.server.ssl.keyPassword=
efm.server.ssl.trustStore=/path/to/truststore.jks
efm.server.ssl.trustStoreType=jks
efm.server.ssl.trustStorePassword=
efm.server.ssl.clientAuth=WANT
```

To enable TLS mutual auth, use the following properties:

```
efm.server.ssl.clientAuth=NEED  # or WANT depending on if it is required

efm.security.user.certificate.enabled=true  # will authenticate trusted client certificates
```


If a client certificate is provided when connecting to the efm server, and no other form of user identity and credentials are present,
then the DN from the client certificate will be used as the user identity.  

### Proxy Authentication

You can configure the server to trust an HTTP reverse proxy to authenticate users externally of the DFM application
and pass the user details with each request. This is useful in SSO environments in which some gateway, proxy, or central web service
handles user authentication to multiple backend services.

Here are the configuration options for using proxy authentication:

```
# Generic SSO Proxy Properties
efm.security.user.proxy.enabled=false # Whether proxy authentication is enabled
efm.security.user.proxy.headerName=x-webauth-user # Case-insensitive header name set by the proxy holding the end user identity.
efm.security.user.proxy.ipWhitelist= # Limit trusted proxy IP addresses to prevent spoofing the user header. Comma-separated or multiple properties using the ipWhitelist[n] syntax.
efm.security.user.proxy.dnWhitelist[0]= # Limit trusted proxy client certificate DNs to prevent spoofing the user header. Use the dnWhitelist[n] syntax as it is common for DNs to contain commas.
efm.security.user.proxy.dnWhitelist[1]=
```

If you are using proxy authentication, it is *strongly* recommended that you use either the DN whitelist or IP whitelist feature
to specify trusted reverse proxies. If you are not using a whitelist, it is assumed you are using some other networking mechanism to
ensure that all authenticated requests are coming from a trusted client, such as only binding DFM to localhost and running the
authenticating proxy on the same machine on a different network interface.

Here is a curl example of passing the proxy user header to the `/api/access` endpoint that returns the current recognized user.

```
curl -H "X-WEBAUTH-USER: alice" http://localhost:10080/efm/api/access

# Response:
{"identity":"alice","anonymous":false}
```

As you can see, this header can be added to any request, which is why DN whitelisting, IP whitelisting, or localhost binding
should be used with the Proxy Authentication feature.

####

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
