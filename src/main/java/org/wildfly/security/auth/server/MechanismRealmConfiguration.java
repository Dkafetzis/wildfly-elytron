/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

package org.wildfly.security.auth.server;

import java.util.List;
import java.util.function.Supplier;

import org.wildfly.common.Assert;

/**
 * A configuration for a single mechanism realm.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MechanismRealmConfiguration {
    private final String realmName;
    private final NameRewriter preRealmRewriter;
    private final NameRewriter postRealmRewriter;
    private final NameRewriter finalRewriter;
    private final Supplier<List<String>> credentialNameSupplier;

    /**
     * Construct a new instance.
     *
     * @param realmName the name of this realm (may not be {@code null})
     * @param preRealmRewriter the pre-realm rewriter to apply (may not be {@code null})
     * @param postRealmRewriter the post-realm rewriter to apply (may not be {@code null})
     * @param finalRewriter the final rewriter to apply (may not be {@code null})
     * @param credentialNameSupplier an optional supplier of credential names to use for this mechanism realm configuration
     */
    MechanismRealmConfiguration(final String realmName, final NameRewriter preRealmRewriter, final NameRewriter postRealmRewriter, final NameRewriter finalRewriter, final Supplier<List<String>> credentialNameSupplier) {
        this.realmName = realmName;
        this.preRealmRewriter = preRealmRewriter;
        this.postRealmRewriter = postRealmRewriter;
        this.finalRewriter = finalRewriter;
        this.credentialNameSupplier = credentialNameSupplier;
    }

    /**
     * Get the mechanism realm name.
     *
     * @return the mechanism realm name (not {@code null})
     */
    public String getRealmName() {
        return realmName;
    }

    /**
     * Get the pre-realm rewriter for this mechanism realm.
     *
     * @return the pre-realm rewriter for this mechanism realm (not {@code null})
     */
    public NameRewriter getPreRealmRewriter() {
        return preRealmRewriter;
    }

    /**
     * Get the post-realm rewriter for this mechanism realm.
     *
     * @return the post-realm rewriter for this mechanism realm (not {@code null})
     */
    public NameRewriter getPostRealmRewriter() {
        return postRealmRewriter;
    }

    /**
     * Get the final rewriter for this mechanism realm.
     *
     * @return the final rewriter for this mechanism realm (not {@code null})
     */
    public NameRewriter getFinalRewriter() {
        return finalRewriter;
    }

    /**
     * Get the credential name supplier, if any.
     *
     * @return the credential name supplier, or {@code null} if none is configured
     */
    public Supplier<List<String>> getCredentialNameSupplier() {
        return credentialNameSupplier;
    }

    /**
     * A realm configuration for no particular realm, which does no additional rewriting.
     */
    public static final MechanismRealmConfiguration NO_REALM = new MechanismRealmConfiguration("none", NameRewriter.IDENTITY_REWRITER, NameRewriter.IDENTITY_REWRITER, NameRewriter.IDENTITY_REWRITER, null);

    /**
     * Obtain a new {@link Builder} capable of building a {@link MechanismRealmConfiguration}.
     *
     * @return a new {@link Builder} capable of building a {@link MechanismRealmConfiguration}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String realmName;
        private NameRewriter preRealmRewriter = NameRewriter.IDENTITY_REWRITER;
        private NameRewriter postRealmRewriter = NameRewriter.IDENTITY_REWRITER;
        private NameRewriter finalRewriter = NameRewriter.IDENTITY_REWRITER;
        private Supplier<List<String>> credentialNameSupplier;

        /**
         * Construct a new instance.
         */
        Builder() {
        }

        public void setRealmName(final String realmName) {
            this.realmName = realmName;
        }

        public void setPreRealmRewriter(final NameRewriter preRealmRewriter) {
            this.preRealmRewriter = preRealmRewriter;
        }

        public void setPostRealmRewriter(final NameRewriter postRealmRewriter) {
            this.postRealmRewriter = postRealmRewriter;
        }

        public void setFinalRewriter(final NameRewriter finalRewriter) {
            this.finalRewriter = finalRewriter;
        }

        public void setCredentialNameSupplier(final Supplier<List<String>> credentialNameSupplier) {
            this.credentialNameSupplier = credentialNameSupplier;
        }

        public MechanismRealmConfiguration build() {
            Assert.checkNotNullParam("realmName", realmName);
            return new MechanismRealmConfiguration(realmName, preRealmRewriter, postRealmRewriter, finalRewriter, credentialNameSupplier);
        }
    }
}