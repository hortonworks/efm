/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.component;

import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.cloudera.cem.efm.service.component.ComponentUtils.isNotNull;

/**
 * Standard implementation of FDRemoteProcessGroupService.
 */
@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDRemoteProcessGroupService extends BaseComponentService<VersionedRemoteProcessGroup,FDRemoteProcessGroup>
        implements FDRemoteProcessGroupService {

    static final String TRANSPORT_PROTOCOL_RAW = "RAW";
    static final String TRANSPORT_PROOTOCOL_HTTP = "HTTP";

    static final String DEFAULT_TRANSPORT_PROTOCOL = TRANSPORT_PROTOCOL_RAW;
    static final String DEFAULT_YIELD_DURATION = "10 sec";
    static final String DEFAULT_COMMUNICATION_TIMEOUT = "30 secs";

    public StandardFDRemoteProcessGroupService(final FDFlowManager flowManager, final ValidationService validationService) {
        super(flowManager, validationService);
    }

    @Override
    public FDRemoteProcessGroup create(final String flowId, final String processGroupId, final VersionedRemoteProcessGroup requestComponentConfig) {
        final String targetUris = requestComponentConfig.getTargetUris();
        if (StringUtils.isBlank(targetUris)) {
            throw new IllegalArgumentException("Cannot add a Remote Process Group without specifying the Target URI(s)");
        }

        validateRequestValues(requestComponentConfig);

        return createComponent(
                flowId,
                processGroupId,
                null,
                requestComponentConfig,
                () -> {
                    final VersionedRemoteProcessGroup componentConfig = new VersionedRemoteProcessGroup();
                    componentConfig.setTransportProtocol(DEFAULT_TRANSPORT_PROTOCOL);
                    componentConfig.setCommunicationsTimeout(DEFAULT_COMMUNICATION_TIMEOUT);
                    componentConfig.setYieldDuration(DEFAULT_YIELD_DURATION);
                    componentConfig.setLocalNetworkInterface("");
                    componentConfig.setProxyHost("");
                    componentConfig.setInputPorts(new HashSet<>());
                    componentConfig.setOutputPorts(new HashSet<>());

                    final FDRemoteProcessGroup component = new FDRemoteProcessGroup();
                    component.setComponentConfiguration(componentConfig);
                    return component;
                },
                VersionedProcessGroup::getRemoteProcessGroups,
                null);
    }

    @Override
    public FDRemoteProcessGroup get(final String flowId, final String componentId) {
        return getComponent(flowId, componentId, VersionedProcessGroup::getRemoteProcessGroups, null);
    }

    @Override
    public FDRemoteProcessGroup update(final String flowId, final VersionedRemoteProcessGroup requestComponentConfig) {
        validateRequestValues(requestComponentConfig);
        return updateComponent(flowId, requestComponentConfig, VersionedProcessGroup::getRemoteProcessGroups, null);
    }

    @Override
    public void delete(final String flowId, final String componentId) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContents = currentFlow.getFlowContent();

        // Locate the remote process group being deleted
        final VersionedRemoteProcessGroup remoteProcessGroup = ComponentUtils.getComponentOrNotFound(
                componentId, flowContents, VersionedProcessGroup::getRemoteProcessGroups);

        final Set<String> remoteInpurtPortTargetIds = remoteProcessGroup.getInputPorts()
                .stream()
                .map(VersionedRemoteGroupPort::getTargetId)
                .collect(Collectors.toSet());

        final Set<String> remoteOutputPortTargetIds = remoteProcessGroup.getOutputPorts()
                .stream()
                .map(VersionedRemoteGroupPort::getTargetId)
                .collect(Collectors.toSet());

        // Locate the process group containing the remote process group
        final VersionedProcessGroup containingGroup = ComponentUtils.getProcessGroupOrIllegalArgument(
                remoteProcessGroup.getGroupIdentifier(), flowContents);

        // Iterate through the connections in the containing group and remove any where the source or destination id
        // is one of the remote input or output ports being removed as part of removing the group
        containingGroup.getConnections().removeIf(connection ->
            remoteInpurtPortTargetIds.contains(connection.getDestination().getId()) || remoteOutputPortTargetIds.contains(connection.getSource().getId()));

        // Continue on to normal deletion of the component...
        deleteComponent(currentFlow, remoteProcessGroup, VersionedProcessGroup::getRemoteProcessGroups);
    }

    @Override
    protected FDRemoteProcessGroup instantiateComponent(final VersionedRemoteProcessGroup componentConfig) {
        final FDRemoteProcessGroup remoteProcessGroup = new FDRemoteProcessGroup();
        remoteProcessGroup.setComponentConfiguration(componentConfig);
        return remoteProcessGroup;
    }

    @Override
    protected void configureComponentSpecifics(final VersionedRemoteProcessGroup requestComponent, final VersionedRemoteProcessGroup resultComponent) {
        final String targetUris = requestComponent.getTargetUris();
        if (isNotNull(targetUris)) {
            final String cleanedTargetUris = StringUtils.join(
                    Arrays.stream(targetUris.split(","))
                            .map(s -> s.trim())
                            .filter(s -> s.length() > 0)
                            .collect(Collectors.toCollection(() -> new LinkedHashSet<>())),
                    ",");

            resultComponent.setTargetUris(cleanedTargetUris);
            resultComponent.setName(cleanedTargetUris);

            if (cleanedTargetUris.contains(",")) {
                final String firstUri = cleanedTargetUris.substring(0, cleanedTargetUris.indexOf(','));
                resultComponent.setTargetUri(firstUri);
            } else {
                resultComponent.setTargetUri(cleanedTargetUris);
            }
        }

        final String commsTimeout = requestComponent.getCommunicationsTimeout();
        if (isNotNull(commsTimeout)) {
            resultComponent.setCommunicationsTimeout(commsTimeout);
        }

        final String yieldDuration = requestComponent.getYieldDuration();
        if (isNotNull(yieldDuration)) {
            resultComponent.setYieldDuration(yieldDuration);
        }

        final String transportProtocol = requestComponent.getTransportProtocol();
        if (isNotNull(transportProtocol)) {
            resultComponent.setTransportProtocol(transportProtocol);
        }

        final String localNetworkInterface = requestComponent.getLocalNetworkInterface();
        if (isNotNull(localNetworkInterface)) {
            resultComponent.setLocalNetworkInterface(localNetworkInterface);
        }

        final String proxyHost = requestComponent.getProxyHost();
        if (isNotNull(proxyHost)) {
            resultComponent.setProxyHost(proxyHost);
        }

        final Integer proxyPort = requestComponent.getProxyPort();
        if (isNotNull(proxyPort)) {
            if (proxyPort < 0) {
                resultComponent.setProxyPort(null);
            } else {
                resultComponent.setProxyPort(proxyPort);
            }
        }
    }

    /**
     * Called before a create or update to validate any values that must be in a specific format before applying the values.
     *
     * @param requestComponent the configuration of the component from the incoming request
     */
    private void validateRequestValues(final VersionedRemoteProcessGroup requestComponent) {
        final String targetUris = requestComponent.getTargetUris();
        if (isNotNull(targetUris)) {
            validateTargetUris(targetUris);
        }

        final String commsTimeout = requestComponent.getCommunicationsTimeout();
        if (isNotNull(commsTimeout)) {
            ComponentUtils.validateTimePeriodValue(commsTimeout);
        }

        final String yieldDuration = requestComponent.getYieldDuration();
        if (isNotNull(yieldDuration)) {
            ComponentUtils.validateTimePeriodValue(yieldDuration);
        }

        final String transportProtocol = requestComponent.getTransportProtocol();
        if (isNotNull(transportProtocol)
                && !transportProtocol.equals(TRANSPORT_PROTOCOL_RAW)
                && !transportProtocol.equals(TRANSPORT_PROOTOCOL_HTTP)) {
            throw new IllegalArgumentException("Unknown transport protocol: " + transportProtocol);
        }

        final Integer proxyPort = requestComponent.getProxyPort();
        if (isNotNull(proxyPort)) {
            if (proxyPort.intValue() < -1 || proxyPort.intValue() == 0 || proxyPort.intValue() > 65535) {
                throw new IllegalArgumentException("Invalid proxy port: " + proxyPort.intValue());
            }
        }
    }

    private static void validateTargetUris(final String targetUris) {
        final Set<String> urls = new LinkedHashSet<>();
        if (targetUris != null && targetUris.length() > 0) {
            Arrays.stream(targetUris.split(","))
                    .map(s -> s.trim())
                    .filter(s -> s.length() > 0)
                    .forEach(s -> {
                        validateUriString(s);
                        urls.add(s);
                    });
        }

        if (urls.size() == 0) {
            throw new IllegalArgumentException("Remote Process Group URIs were not specified.");
        }

        final Predicate<String> isHttps = url -> url.toLowerCase().startsWith("https:");
        if (urls.stream().anyMatch(isHttps) && urls.stream().anyMatch(isHttps.negate())) {
            throw new IllegalArgumentException("Different protocols are used in the remote process group URIs " + targetUris);
        }
    }

    private static void validateUriString(String s) {
        // parse the uri
        final URI uri;
        try {
            uri = URI.create(s);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("The specified remote process group URI is malformed: " + s);
        }

        // validate each part of the uri
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("The specified remote process group URI is malformed: " + s);
        }

        if (!(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("The specified remote process group URI is invalid because it is not http or https: " + s);
        }
    }

}
