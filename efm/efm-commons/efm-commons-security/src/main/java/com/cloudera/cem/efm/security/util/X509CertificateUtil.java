/*
 * Apache NiFi - MiNiFi
 * Copyright 2014-2018 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.security.util;

import org.apache.nifi.registry.security.authentication.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * Utility methods for X509 client certificates on HttpServletRequests.
 */
public class X509CertificateUtil {

    private static final Logger logger = LoggerFactory.getLogger(X509CertificateUtil.class);

    /**
     * Extract the first client certificate from the specified HttpServletRequest or null if none are present.
     *
     * @param request http request
     * @return cert
     */
    public static X509Certificate extractClientCertificate(HttpServletRequest request) {
        X509Certificate[] certs = extractClientCertificates(request);
        if (certs != null && certs.length > 0) {
            return certs[0];
        }
        return null;
    }

    /**
     * Extract all certificates from the specified HttpServletRequest or null if none are present.
     *
     * @param request http request
     * @return certs
     */
    public static X509Certificate[] extractClientCertificates(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            logger.debug("No client certificates found in request.");
            return null;
        }
        return certs;
    }

    public static void validateClientCertificateOrThrow(X509Certificate certificate) {
        if (certificate == null) {
            throw new InvalidCredentialsException("Client certificate required but not present on request.");
        }

        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException cee) {
            final String message = String.format("Client certificate for (%s) is expired.", certificate.getSubjectDN().getName());
            logger.warn(message, cee);
            throw new InvalidCredentialsException(message, cee);
        } catch (CertificateNotYetValidException cnyve) {
            final String message = String.format("Client certificate for (%s) is not yet valid.", certificate.getSubjectDN().getName());
            logger.warn(message, cnyve);
            throw new InvalidCredentialsException(message, cnyve);
        } catch (final Exception e) {
            logger.warn(e.getMessage(), e);
            throw new InvalidCredentialsException(e.getMessage(), e);
        }
    }



}
