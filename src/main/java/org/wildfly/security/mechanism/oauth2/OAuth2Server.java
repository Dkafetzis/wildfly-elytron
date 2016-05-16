/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.mechanism.oauth2;

import org.wildfly.security._private.ElytronMessages;
import org.wildfly.security.auth.callback.EvidenceVerifyCallback;
import org.wildfly.security.evidence.BearerTokenEvidence;
import org.wildfly.security.mechanism.AuthenticationMechanismException;
import org.wildfly.security.mechanism.MechanismUtil;
import org.wildfly.security.util.ByteIterator;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.wildfly.security._private.ElytronMessages.log;

/**
 * An OAuth2 Sasl Server based on RFC-7628.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class OAuth2Server {

    public static final String CONFIG_OPENID_CONFIGURATION_URL = "openid-configuration";
    private static final String KV_DELIMITER = "%x01";

    private final String mechanismName;
    private final CallbackHandler callbackHandler;
    private final Map<String, ?> serverConfig;

    public OAuth2Server(String mechanismName, CallbackHandler callbackHandler, Map<String, ?> serverConfig) {
        this.mechanismName = mechanismName;
        this.callbackHandler = callbackHandler;
        this.serverConfig = serverConfig;
    }

    public OAuth2InitialClientMessage parseInitialClientMessage(byte[] fromBytes) throws AuthenticationMechanismException {
        byte[] messageBytes = fromBytes.clone();
        ByteIterator byteIterator = ByteIterator.ofBytes(fromBytes.clone());

        try {
            final char cbindFlag = (char) byteIterator.next();

            if (cbindFlag != 'n') {
                throw ElytronMessages.log.mechChannelBindingNotSupported(this.mechanismName);
            }

            String authorizationID = null;

            if (byteIterator.next() == ',') {
                final int c = byteIterator.next();

                if (c == 'a') {
                    if (byteIterator.next() != '=') {
                        throw log.mechInvalidClientMessage(this.mechanismName);
                    }
                    authorizationID = byteIterator.delimitedBy(',').asUtf8String().drainToString();
                    if (byteIterator.next() != ',') {
                        throw ElytronMessages.log.mechInvalidClientMessage(this.mechanismName);
                    }
                }
            }

            String auth = getValue("auth", byteIterator.asUtf8String().drainToString());

            if (auth == null) {
                throw log.mechInvalidClientMessage(this.mechanismName);
            }

            return new OAuth2InitialClientMessage(authorizationID, auth, messageBytes);
        } catch (NoSuchElementException ignored) {
            throw ElytronMessages.log.mechInvalidMessageReceived(this.mechanismName);
        }
    }

    private String getValue(String key, String keyValuesPart) {
        for (String current : keyValuesPart.split(KV_DELIMITER)) {
            String[] keyValue = current.split("=");

            if (keyValue[0].equals(key)) {
                return keyValue[1];
            }
        }

        return null;
    }

    public byte[] evaluateInitialResponse(OAuth2InitialClientMessage initialClientMessage) throws AuthenticationMechanismException {
        if (initialClientMessage.isBearerToken()) {
            String auth = initialClientMessage.getAuth();
            String token = auth.substring(auth.indexOf(" ") + 1);
            BearerTokenEvidence evidence = new BearerTokenEvidence(token);
            EvidenceVerifyCallback evidenceVerifyCallback = new EvidenceVerifyCallback(evidence);

            try {
                MechanismUtil.handleCallbacks(this.mechanismName, this.callbackHandler, evidenceVerifyCallback);
            } catch (UnsupportedCallbackException e) {
                throw log.mechAuthorizationUnsupported(this.mechanismName, e);
            }

            // successful verification, token is supposed to be valid and just respond with an empty message
            if (evidenceVerifyCallback.isVerified()) {
                return new byte[0];
            }

            return createErrorMessage();
        }

        throw log.mechInvalidClientMessage(this.mechanismName);
    }

    private byte[] createErrorMessage() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        objectBuilder.add("status", "invalid_token");

        Object asDiscoveryUrl = serverConfig.get(CONFIG_OPENID_CONFIGURATION_URL);

        if (asDiscoveryUrl != null) {
            objectBuilder.add(CONFIG_OPENID_CONFIGURATION_URL, asDiscoveryUrl.toString());
        }

        return ByteIterator.ofBytes(objectBuilder.build().toString().getBytes()).base64Encode().asUtf8().drain();
    }
}
