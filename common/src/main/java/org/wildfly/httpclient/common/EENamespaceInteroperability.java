/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
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
package org.wildfly.httpclient.common;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

import static org.wildfly.httpclient.common.Protocol.VERSION_ONE_PATH;
import static org.wildfly.httpclient.common.Protocol.VERSION_TWO_PATH;

/**
 * EE namespace interoperability implementation for allowing Jakarta EE namespace servers and clients communication with
 * Javax EE namespace endpoints.
 *
 * EE namespace interoperability must be enabled on all Jakarta servers and clients to make communication
 * among them possible.
 *
 * @author Flavia Rainone
 * @author Richard Opalka
 */
final class EENamespaceInteroperability { // TODO: eliminate this class?

    private EENamespaceInteroperability() {}

    /**
     * Wraps the HTTP server handler into an EE namespace interoperable handler. Such handler implements the
     * EE namespace interoperability at the server side before delegating to the wrapped {@code httpHandler}
     *
     * @param httpHandler the handler to be wrapped
     * @return handler the ee namespace interoperability handler
     */
    static HttpHandler createInteroperabilityHandler(HttpHandler httpHandler) { // TODO: eliminate this method?
        final PathHandler versionPathHandler = new PathHandler();
        versionPathHandler.addPrefixPath(VERSION_ONE_PATH, httpHandler);
        versionPathHandler.addPrefixPath(VERSION_TWO_PATH, httpHandler);
        return versionPathHandler;
    }

}
