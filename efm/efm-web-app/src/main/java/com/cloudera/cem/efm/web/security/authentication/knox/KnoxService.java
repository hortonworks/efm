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
package com.cloudera.cem.efm.web.security.authentication.knox;

import com.cloudera.cem.efm.annotation.ConditionalOnKnox;
import com.cloudera.cem.efm.web.security.authentication.exception.KnoxAuthenticationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@ConditionalOnKnox
public class KnoxService {

    private static final Logger logger = LoggerFactory.getLogger(KnoxService.class);

    private final KnoxConfiguration configuration;
    private JWSVerifier verifier;
    private String knoxUrl;
    private Set<String> audiences;

    /**
     * Creates a new KnoxService.
     *
     * @param configuration          knox configuration
     */
    @Autowired
    public KnoxService(final KnoxConfiguration configuration) {
        this.configuration = configuration;

        // if knox sso support is enabled, validate the configuration
        if (configuration.isEnabled()) {
            // ensure the url is provided
            knoxUrl = configuration.getUrl();
            if (StringUtils.isBlank(knoxUrl)) {
                throw new RuntimeException("Knox URL is required when Apache Knox SSO support is enabled.");
            }

            // ensure the cookie name is set
            if (StringUtils.isBlank(configuration.getCookieName())) {
                throw new RuntimeException("Knox Cookie Name is required when Apache Knox SSO support is enabled.");
            }

            // create the verifier
            if (StringUtils.isBlank(configuration.getPublicKey())) {
                throw new RuntimeException("Knox Public Key Path is required when Apache Knox SSO support is enabled.");
            }
            verifier = new RSASSAVerifier(getRSAPublicKey(configuration.getPublicKey()));

            // get the audience
            audiences = configuration.getAudiences();
        }
    }

    /**
     * Returns whether Knox support is enabled.
     *
     * @return whether Knox support is enabled
     */
    public boolean isKnoxEnabled() {
        return configuration.isEnabled();
    }

    /**
     * Returns the Knox Url.
     *
     * @return knox url
     */
    public String getKnoxUrl() {
        if (!configuration.isEnabled()) {
            throw new IllegalStateException("Apache Knox SSO is not enabled.");
        }

        return knoxUrl;
    }

    public String getKnoxCookieName() {
        if (!configuration.isEnabled()) {
            throw new IllegalStateException("Apache Knox SSO is not enabled.");
        }

        return configuration.getCookieName();
    }

    /**
     * Extracts the authentication from the token and verify it.
     *
     * @param jwt signed jwt string
     * @return the user authentication
     * @throws ParseException if the payload of the jwt doesn't represent a valid json object and a jwt claims set
     * @throws com.nimbusds.jose.JOSEException if the JWS object couldn't be verified
     */
    public String getAuthenticationFromToken(final String jwt) throws ParseException, JOSEException {
        if (!configuration.isEnabled()) {
            throw new IllegalStateException("Apache Knox SSO is not enabled.");
        }

        // attempt to parse the signed jwt
        final SignedJWT signedJwt = SignedJWT.parse(jwt);

        // validate the token
        if (validateToken(signedJwt)) {
            final JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();
            if (claimsSet == null) {
                logger.info("Claims set is missing from Knox JWT.");
                throw new KnoxAuthenticationException("The Knox JWT token is not valid.", knoxUrl);
            }

            // extract the user identity from the token
            return claimsSet.getSubject();
        } else {
            throw new KnoxAuthenticationException("The Knox JWT token is not valid.", knoxUrl);
        }
    }

    /**
     * Validate the specified jwt.
     *
     * @param jwtToken knox jwt
     * @return whether this jwt is valid
     * @throws JOSEException if the jws object couldn't be verified
     * @throws ParseException if the payload of the jwt doesn't represent a valid json object and a jwt claims set
     */
    private boolean validateToken(final SignedJWT jwtToken) throws JOSEException, ParseException {
        final boolean validSignature = validateSignature(jwtToken);
        final boolean validAudience = validateAudience(jwtToken);
        final boolean notExpired = validateExpiration(jwtToken);

        return validSignature && validAudience && notExpired;
    }

    /**
     * Validate the jwt signature.
     *
     * @param jwtToken knox jwt
     * @return whether this jwt signature is valid
     * @throws JOSEException if the jws object couldn't be verified
     */
    private boolean validateSignature(final SignedJWT jwtToken) throws JOSEException {
        boolean valid = false;

        // ensure the token is signed
        if (JWSObject.State.SIGNED.equals(jwtToken.getState())) {

            // ensure the signature is present
            if (jwtToken.getSignature() != null) {

                // verify the token
                valid = jwtToken.verify(verifier);
            }
        }

        if (!valid) {
            logger.error("The Knox JWT has an invalid signature.");
        }

        return valid;
    }

    /**
     * Validate the jwt audience.
     *
     * @param jwtToken knox jwt
     * @return whether this jwt audience is valid
     * @throws ParseException if the payload of the jwt doesn't represent a valid json object and a jwt claims set
     */
    private boolean validateAudience(final SignedJWT jwtToken) throws ParseException {
        if (audiences == null) {
            return true;
        }

        final JWTClaimsSet claimsSet = jwtToken.getJWTClaimsSet();
        if (claimsSet == null) {
            logger.error("Claims set is missing from Knox JWT.");
            return false;
        }

        final List<String> tokenAudiences = claimsSet.getAudience();
        if (tokenAudiences == null) {
            logger.error("Audience is missing from the Knox JWT.");
            return false;
        }

        boolean valid = false;
        for (final String tokenAudience : tokenAudiences) {
            // ensure one of the audiences is matched
            if (audiences.contains(tokenAudience)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            logger.error(String.format("The Knox JWT does not have the required audience(s). Required one of [%s]. Present in JWT [%s].",
                    StringUtils.join(audiences, ", "), StringUtils.join(tokenAudiences, ", ")));
        }

        return valid;
    }

    /**
     * Validate the jwt expiration.
     *
     * @param jwtToken knox jwt
     * @return whether this jwt is not expired
     * @throws ParseException if the payload of the jwt doesn't represent a valid json object and a jwt claims set
     */
    private boolean validateExpiration(final SignedJWT jwtToken) throws ParseException {
        boolean valid = false;

        final JWTClaimsSet claimsSet = jwtToken.getJWTClaimsSet();
        if (claimsSet == null) {
            logger.error("Claims set is missing from Knox JWT.");
            return false;
        }

        final Date now = new Date();
        final Date expiration = claimsSet.getExpirationTime();

        // the token is not expired if the expiration isn't present or the expiration is after now
        if (expiration == null || now.before(expiration)) {
            valid = true;
        }

        if (!valid) {
            logger.error("The Knox JWT is expired.");
        }

        return valid;
    }

    private static RSAPublicKey getRSAPublicKey(final String publicKeyPath) {
        // get the path to the public key
        final Path knoxPublicKeyPath = Paths.get(publicKeyPath);

        // ensure the file exists
        if (Files.isRegularFile(knoxPublicKeyPath) && Files.exists(knoxPublicKeyPath)) {
            try (final InputStream publicKeyStream = Files.newInputStream(knoxPublicKeyPath)) {
                final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                final X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(publicKeyStream);
                return (RSAPublicKey) certificate.getPublicKey();
            } catch (final IOException | CertificateException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new RuntimeException(String.format("The specified Knox public key path does not exist '%s'", knoxPublicKeyPath.toString()));
        }
    }

}
