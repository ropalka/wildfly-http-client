/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.httpclient.naming;

import static org.wildfly.httpclient.naming.Constants.HTTP_SCHEME;
import static org.wildfly.httpclient.naming.Constants.HTTPS_SCHEME;

import javax.naming.NamingException;

import org.wildfly.naming.client.NamingProvider;
import org.wildfly.naming.client.NamingProviderFactory;
import org.wildfly.naming.client.ProviderEnvironment;
import org.wildfly.naming.client.util.FastHashtable;

/**
 * @author Stuart Douglas
 */
public class HttpNamingProviderFactory implements NamingProviderFactory {
    /**
     * Construct a new instance.
     */
    public HttpNamingProviderFactory() {
    }

    @Override
    public boolean supportsUriScheme(String providerScheme, FastHashtable<String, Object> env) {
        switch (providerScheme) {
            case HTTP_SCHEME:
            case HTTPS_SCHEME:
                return true;
        }
        return false;
    }

    @Override
    public NamingProvider createProvider(final FastHashtable<String, Object> env, final ProviderEnvironment providerEnvironment) throws NamingException {
        if (providerEnvironment.getProviderUris().size() == 0) {
            throw HttpNamingClientMessages.MESSAGES.atLeastOneUri();
        }
        // TODO: examine env for security information to override invocation-time lookup
        return new HttpNamingProvider(providerEnvironment);
    }
}
