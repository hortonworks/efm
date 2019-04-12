#!/bin/sh -e
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

# Incorporate helper functions
. /opt/commons/commons.sh

# NIFI_HOME is defined by an ENV command in the backing Dockerfile
export properties_file=${NIFI_HOME}/conf/nifi.properties
echo "" >> ${properties_file}                         # There is an escape character at the end of nifi.properties that is causing the first >> to be ineffective.
echo "\n# Appended Properties" >> ${properties_file}  # Before adding this blank line and heading, the first call to prop_replace_or_add did not work.

prop_replace_or_add 'nifi.web.http.port.forwarding'              "${NIFI_WEB_HTTP_PORT_FORWARDING}"
prop_replace_or_add 'nifi.web.https.port.forwarding'             "${NIFI_WEB_HTTPS_PORT_FORWARDING}"

prop_replace_or_add 'nifi.remote.input.host'                     "${NIFI_REMOTE_INPUT_HOST:-$HOSTNAME}"
prop_replace_or_add 'nifi.remote.input.socket.port'              "${NIFI_REMOTE_INPUT_SOCKET_PORT:-10000}"
prop_replace_or_add 'nifi.remote.input.secure'                   "${NIFI_REMOTE_INPUT_SECURE:-false}"
prop_replace_or_add 'nifi.remote.input.http.enabled'             "${NIFI_REMOTE_INPUT_HTTP_ENABLED:-true}"
prop_replace_or_add 'nifi.remote.input.http.transaction.ttl'     "${NIFI_REMOTE_INPUT_HTTP_TRANSACTION_TTL:-30 sec}"
prop_replace_or_add 'nifi.remote.contents.cache.expiration'      "${NIFI_REMOTE_CONTENTS_CACHE_EXPIRATION:-30 sec}"

if [ -n "${NIFI_REMOTE_ROUTE_HTTP_INTERNAL_WHEN}" ]; then
    prop_replace_or_add 'nifi.remote.route.http.internal.when'       "${NIFI_REMOTE_ROUTE_HTTP_INTERNAL_WHEN}"
    prop_replace_or_add 'nifi.remote.route.http.internal.hostname'   "${NIFI_REMOTE_ROUTE_HTTP_INTERNAL_HOSTNAME}"
    prop_replace_or_add 'nifi.remote.route.http.internal.port'       "${NIFI_REMOTE_ROUTE_HTTP_INTERNAL_PORT}"
    prop_replace_or_add 'nifi.remote.route.http.internal.secure'     "${NIFI_REMOTE_ROUTE_HTTP_INTERNAL_SECURE}"
fi

if [ -n "${NIFI_REMOTE_ROUTE_HTTP_EXTERNAL_WHEN}" ]; then
    prop_replace_or_add 'nifi.remote.route.http.external.when'       "${NIFI_REMOTE_ROUTE_HTTP_EXTERNAL_WHEN}"
    prop_replace_or_add 'nifi.remote.route.http.external.hostname'   "${NIFI_REMOTE_ROUTE_HTTP_EXTERNAL_HOSTNAME}"
    prop_replace_or_add 'nifi.remote.route.http.external.port'       "${NIFI_REMOTE_ROUTE_HTTP_EXTERNAL_PORT}"
    prop_replace_or_add 'nifi.remote.route.http.external.secure'     "${NIFI_REMOTE_ROUTE_HTTP_EXTERNAL_SECURE}"
fi

if [ -n "${NIFI_REMOTE_ROUTE_RAW_INTERNAL_WHEN}" ]; then
    prop_replace_or_add 'nifi.remote.route.raw.internal.when'        "${NIFI_REMOTE_ROUTE_RAW_INTERNAL_WHEN}"
    prop_replace_or_add 'nifi.remote.route.raw.internal.hostname'    "${NIFI_REMOTE_ROUTE_RAW_INTERNAL_HOSTNAME}"
    prop_replace_or_add 'nifi.remote.route.raw.internal.port'        "${NIFI_REMOTE_ROUTE_RAW_INTERNAL_PORT}"
    prop_replace_or_add 'nifi.remote.route.raw.internal.secure'      "${NIFI_REMOTE_ROUTE_RAW_INTERNAL_SECURE}"
fi

if [ -n "${NIFI_REMOTE_ROUTE_RAW_EXTERNAL_WHEN}" ]; then
    prop_replace_or_add 'nifi.remote.route.raw.external.when'        "${NIFI_REMOTE_ROUTE_RAW_EXTERNAL_WHEN}"
    prop_replace_or_add 'nifi.remote.route.raw.external.hostname'    "${NIFI_REMOTE_ROUTE_RAW_EXTERNAL_HOSTNAME}"
    prop_replace_or_add 'nifi.remote.route.raw.external.port'        "${NIFI_REMOTE_ROUTE_RAW_EXTERNAL_PORT}"
    prop_replace_or_add 'nifi.remote.route.raw.external.secure'      "${NIFI_REMOTE_ROUTE_RAW_EXTERNAL_SECURE}"
fi

# Allow the container to be accessed via its hostname or name applied by Docker service definition
export NIFI_WEB_HTTP_HOST=''

/opt/nifi/scripts/start.sh
