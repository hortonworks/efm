/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.metrics;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Name {

    public static final String SEPARATOR = ".";
    private static final String SEPARATOR_REGEX_PATTERN = Pattern.quote(SEPARATOR);

    public static final String C2_HEARTBEAT_COUNT = join(Prefix.C2_HEARTBEAT, "count");
    public static final String C2_HEARTBEAT_CONTENT_LENGTH = join(Prefix.C2_HEARTBEAT, "contentLength");
    public static final String C2_HEARTBEAT_LAST_SEEN_TIME = join(Prefix.C2_HEARTBEAT, "lastSeenTime");

    /**
     * Given the String parts of the meter name, returns a single String with the parts joined by Name.SEPARATOR.
     *
     * This is useful for dynamic, procedurally generated meter names.
     *
     * @param parts the parts from which to create a meter name.
     * @return the meter name
     */
    public static String join(final String... parts) {
        return StringUtils.join(parts, SEPARATOR);
    }

    /**
     * Given a meter name, returns all prefixes it contains, for the purpose of meter name matching.
     *
     * For example, if the meter name is {@code com.example.app.meter}, this method would return
     * {@code com}, {@code com.example}, {@code com.example.app}, {@code com.example.app.meter},
     *
     * @param meterName the name of the meter for which to extract all possible prefixes
     * @return the list of prefixes for the given meter name
     */
    public static List<String> prefixesOf(final String meterName) {
        final String[] parts = meterName.split(SEPARATOR_REGEX_PATTERN);

        final List<String> prefixes = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            final String nextPrefix = i > 0 ? join(prefixes.get(i-1), parts[i]) : parts[i];
            prefixes.add(nextPrefix);
        }
        return prefixes;
    }


    public static class Prefix {
        public static final String C2 = "efm";

        public static final String C2_REPO = "efm.repo";

        public static final String C2_AGENT_MANIFEST = "efm.agentManifest";
        public static final String C2_AGENT_CLASS = "efm.agentClass";
        public static final String C2_AGENT = "efm.agent";
        public static final String C2_DEVICE = "efm.device";
        public static final String C2_EVENT = "efm.event";
        public static final String C2_FLOW = "efm.flow";
        public static final String C2_HEARTBEAT = "efm.heartbeat";
        public static final String C2_OPERATION = "efm.operation";

        public static final String C2_AGENT_STATUS_REPO_FLOWFILE = "efm.agentStatus.repo.flowfile";
        public static final String C2_AGENT_STATUS_REPO_PROVENANCE = "efm.agentStatus.repo.provenance";
        public static final String C2_FLOW_STATUS_QUEUE = "efm.flowStatus.queue";
    }

    public static class Suffix {
        public static final String SIZE = "size";
        public static final String SIZE_MAX = "sizeMax";
        public static final String SIZE_USAGE = "sizeUsage";
        public static final String DATA_SIZE = "dataSize";
        public static final String DATA_SIZE_MAX = "dataSizeMax";
        public static final String DATE_SIZE_USAGE = "dataSizeUsage";
    }

    public static class Tag {
        public static final String EMPTY_VALUE = "";

        public static final String C2_HOST = "efmHost";

        public static final String DEVICE_ID = "deviceId";
        public static final String AGENT_ID = "agentId";
        public static final String AGENT_CLASS = "agentClass";
        public static final String AGENT_MANIFEST_ID = "agentManifestId";
        public static final String FLOW_ID = "flowId";

        public static final String[] ALL = {C2_HOST, DEVICE_ID, AGENT_ID, AGENT_CLASS, AGENT_MANIFEST_ID, FLOW_ID};
    }

}
