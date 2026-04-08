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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.AbstractAttachable;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.wildfly.httpclient.common.HeadersHelper.addResponseHeader;
import static org.wildfly.httpclient.common.HeadersHelper.getRequestHeader;
import static org.wildfly.httpclient.common.HttpMarshallerFactory.DEFAULT_FACTORY;
import static org.wildfly.httpclient.common.HttpMarshallerFactory.INTEROPERABLE_FACTORY;
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
final class EENamespaceInteroperability {
    /**
     * Indicates if EE namespace interoperable mode is enabled.
     */
    static final boolean EE_NAMESPACE_INTEROPERABLE_MODE = Boolean.parseBoolean(
            WildFlySecurityManager.getPropertyPrivileged("org.wildfly.ee.namespace.interop", "false"));

    // header indicating the protocol version mode that is being used by the request/response sender
    private static final HttpString PROTOCOL_VERSION = new HttpString("x-wf-version");
    // value for PROTOCOL_VERSION header: used to handshake a higher version, only when both ends use EE jakarta namespace
    private static final String LATEST_VERSION = String.valueOf(Protocol.LATEST);
    // key used to attach http marshaller factory to a client request / server exchange
    private static final AttachmentKey<HttpMarshallerFactory> HTTP_MARSHALLER_FACTORY_KEY = AttachmentKey.create(HttpMarshallerFactory.class);
    // key used to attach an http unmarshaller factory to a server exchange
    private static final AttachmentKey<HttpMarshallerFactory> HTTP_UNMARSHALLER_FACTORY_KEY = AttachmentKey.create(HttpMarshallerFactory.class);

    static {
        if (EE_NAMESPACE_INTEROPERABLE_MODE) {
            HttpClientMessages.MESSAGES.javaeeToJakartaeeBackwardCompatibilityLayerInstalled();
        }
    }

    private EENamespaceInteroperability() {}

    /**
     * Wraps the HTTP server handler into an EE namespace interoperable handler. Such handler implements the
     * EE namespace interoperability at the server side before delegating to the wrapped {@code httpHandler}
     *
     * @param httpHandler the handler to be wrapped
     * @return handler the ee namespace interoperability handler
     */
    static HttpHandler createInteroperabilityHandler(HttpHandler httpHandler) {
        return createProtocolVersionHttpHandler(new EENamespaceInteroperabilityHandler(httpHandler), new JakartaNamespaceHandler(httpHandler));
    }

    static HttpHandler createProtocolVersionHttpHandler(HttpHandler interoperabilityHandler, HttpHandler latestProtocolHandler) {
        final PathHandler versionPathHandler = new PathHandler();
        versionPathHandler.addPrefixPath(VERSION_ONE_PATH, interoperabilityHandler);
        versionPathHandler.addPrefixPath(VERSION_TWO_PATH, latestProtocolHandler);
        return versionPathHandler;
    }

    /**
     * Returns the HTTPMarshallerFactoryProvider instance responsible for taking care of marshalling
     * and unmarshalling according to the values negotiated by the ee namespace interoperability headers.
     *
     * @return the HTTPMarshallerFactoryProvider. All marshalling and unmarshalling done at both server
     * and client side have to be done through a factory provided by this object.
     */
    static HttpMarshallerFactoryProvider getHttpMarshallerFactoryProvider() {
        return new HttpMarshallerFactoryProvider() {
            @Override
            public HttpMarshallerFactory getMarshallerFactory(AbstractAttachable attachable) {
                return attachable.getAttachment(HTTP_MARSHALLER_FACTORY_KEY);
            }

            @Override
            public HttpMarshallerFactory getUnmarshallerFactory(AbstractAttachable attachable) {
                return attachable.getAttachment(HTTP_UNMARSHALLER_FACTORY_KEY);
            }
        };
    }

    /*
    Server side EE namespace interoperability
     */

    private static class EENamespaceInteroperabilityHandler implements HttpHandler {

        private final HttpHandler next;

        EENamespaceInteroperabilityHandler(HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (LATEST_VERSION.equals(getRequestHeader(exchange, PROTOCOL_VERSION))) {
                // respond that this end also supports version two
                addResponseHeader(exchange, PROTOCOL_VERSION, LATEST_VERSION);
                // transformation is required for unmarshalling because client is on EE namespace interoperable mode
                exchange.putAttachment(HTTP_UNMARSHALLER_FACTORY_KEY, INTEROPERABLE_FACTORY);
                // no transformation required for marshalling, server is sending response in Jakarta
                exchange.putAttachment(HTTP_MARSHALLER_FACTORY_KEY, DEFAULT_FACTORY);
            } else {
                // transformation is required for unmarshalling request and marshalling response,
                // because server is interoperable mode and the lack of a header indicates this is
                // either a Javax EE client or a Jakarta EE client that is not interoperable
                // the latter case will lead to an error when unmarshalling at client side)
                exchange.putAttachment(HTTP_MARSHALLER_FACTORY_KEY, INTEROPERABLE_FACTORY);
                exchange.putAttachment(HTTP_UNMARSHALLER_FACTORY_KEY, INTEROPERABLE_FACTORY);
            }
            next.handleRequest(exchange);
        }
    }

    private static class JakartaNamespaceHandler implements HttpHandler {

        private final HttpHandler next;

        JakartaNamespaceHandler(HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            // no transformation required whatsoever, just make sure we have a factory set
            // or else we will see a NPE when trying to use those attachments
            exchange.putAttachment(HTTP_UNMARSHALLER_FACTORY_KEY, DEFAULT_FACTORY);
            exchange.putAttachment(HTTP_MARSHALLER_FACTORY_KEY, DEFAULT_FACTORY);
            next.handleRequest(exchange);
        }
    }
}
