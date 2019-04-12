# (c) 2018-2019 Cloudera, Inc. All rights reserved.
#
#  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
#  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
#  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
#  properly licensed third party, you do not have any rights to this code.
#
#  If this code is provided to you under the terms of the AGPLv3:
#   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
#   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
#       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
#   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
#       FROM OR RELATED TO THE CODE; AND
#   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
#       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
#       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
#       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.

.PHONY: efm-docker dfo-docker minificpp-docker test-nifi-docker \
	compose_file env-up env-down env-logs env-shell scale-agent \
	clean clean-compose-file clean-docker

PROXY=on
compose_file = docker/test/docker-compose.yml
compose_command = docker-compose -f $(compose_file)

efm_version = 1.0.0-SNAPSHOT
efm_root_pom = efm/pom.xml
efm_assembly = efm/efm-assembly/target/efm-$(efm_version)-bin.tar.gz

docker_efm_tag = hortonworks/efm
docker_cpp_tag = hortonworks/test-nifi-minifi-cpp
docker_java_tag = hortonworks/test-nifi-minifi-java
docker_nifi_tag = hortonworks/test-nifi
docker_prometheus_tag = quay.io/prometheus/prometheus
docker_grafana_tag = grafana/grafana
docker_traefik_tag = traefik

SOURCE_COMMIT = $(shell git rev-parse HEAD)

$(efm_assembly):
	mvn install -DskipTests -T2 -f $(efm_root_pom)

efm-docker: $(efm_assembly)
	docker build --build-arg SRC_COMMIT_HASH=$(SOURCE_COMMIT) --tag $(docker_efm_tag):latest -f docker/efm-image/Dockerfile .

minificpp-docker:
	docker build --pull --tag $(docker_cpp_tag):latest -f docker/test/minificpp-image/Dockerfile  .

minifijava-docker:
	docker build --pull --tag $(docker_java_tag):latest -f docker/test/minifijava-image/Dockerfile  .

test-nifi-docker:
	docker build --tag $(docker_nifi_tag):latest -f docker/test/nifi-test-image/Dockerfile  .

$(compose_file):
ifeq ($(PROXY),on)
	docker-compose -f docker/test/docker-compose-base.yml -f docker/test/docker-compose-proxy.yml config > $(compose_file)
else
	docker-compose -f docker/test/docker-compose-base.yml config > $(compose_file)
endif

compose-file: $(compose_file)

env-up: compose-file efm-docker minificpp-docker minifijava-docker test-nifi-docker
	$(compose_command) up --build -d

env-down: compose-file
	$(compose_command) down

env-logs: compose-file
	$(compose_command) logs -f $(svc)

env-shell: compose-file
	$(compose_command) exec $(svc) /bin/sh

scale-agent: compose-file
	$(compose_command) up --scale agent=$(AGENT_COUNT) -d agent

clean:
	mvn clean -f $(efm_root_pom)

clean-compose-file:
	rm -f $(compose_file)

clean-docker: env-down clean-compose-file
	docker rmi -f $(docker_efm_tag)
	docker rmi -f $(docker_cpp_tag)
	docker rmi -f $(docker_nifi_tag)
	docker rmi -f $(docker_prometheus_tag)
	docker rmi -f $(docker_grafana_tag)
	docker rmi -f $(docker_traefik_tag)
