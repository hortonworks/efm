/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the
 * terms of the Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with
 * a third party authorized to distribute this code.  If you do not have a written agreement with Cloudera
 * or with an authorized and properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS
 *      CODE, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT,
 *      MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS
 *      FOR ANY CLAIMS ARISING FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA
 *      IS NOT LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL
 *      DAMAGES INCLUDING, BUT NOT LIMITED TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF
 *      INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.web.security;

import com.cloudera.cem.efm.security.authentication.proxy.ProxyIdentityProviderProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class C2SecurityGuard {

    private final ServerProperties serverProperties;
    private final ProxyIdentityProviderProperties proxyIdentityProviderProperties;

    @Autowired
    public C2SecurityGuard(
            final C2SecurityGuardProperties securityGuardProperties,
            final ServerProperties serverProperties,
            final ProxyIdentityProviderProperties proxyIdentityProviderProperties)
    throws IllegalStateException {

        this.serverProperties = serverProperties;
        this.proxyIdentityProviderProperties = proxyIdentityProviderProperties;

        // If the SecurityGuard is enabled, run the default security rule checks
        // TODO in the future, the SecurityGuard could provider per-rule configurability or coarse "high", "medium", and "low" settings
        if (securityGuardProperties.isEnabled()) {
            validateState();
            verifyProxyAuthConfigProperties();
        }
    }

    private void validateState() {
        if (this.serverProperties == null) {
            throw new IllegalStateException("C2SecurityGuard requires ServerProperties but was null");
        }
        if (this.proxyIdentityProviderProperties == null) {
            throw new IllegalStateException("C2SecurityGuard requires ProxyIdentityProviderProperties but was null");
        }
    }

    private void verifyProxyAuthConfigProperties() {
        if (proxyIdentityProviderProperties.isEnabled() && proxyHeaderIsAlwaysTrusted() && serverAddressNotLoopback()) {
            throw new IllegalStateException("C2SecurityGuard check verifyProxyAuthConfigProperties failed. " +
                    "When using externalized proxy header authentication, the proxy must be trusted using an IP or DN whitelist, " +
                    "or the server must be bound to a loopback (localhost) address. Check your c2.properties file.");
        }
    }

    private boolean proxyHeaderIsAlwaysTrusted() {
        return CollectionUtils.isEmpty(proxyIdentityProviderProperties.getDnWhitelist())
                && CollectionUtils.isEmpty(proxyIdentityProviderProperties.getIpWhitelist());
    }

    private boolean serverAddressNotLoopback() {
        return !serverProperties.getAddress().isLoopbackAddress();
    }

}
