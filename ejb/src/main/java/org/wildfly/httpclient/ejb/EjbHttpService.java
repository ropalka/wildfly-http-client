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

package org.wildfly.httpclient.ejb;

import static org.wildfly.httpclient.ejb.RequestType.CANCEL;
import static org.wildfly.httpclient.ejb.RequestType.CREATE_SESSION;
import static org.wildfly.httpclient.ejb.RequestType.DISCOVER;
import static org.wildfly.httpclient.ejb.RequestType.INVOKE;

import io.undertow.conduits.GzipStreamSourceConduit;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.AllowedMethodsHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.encoding.RequestEncodingHandler;
import io.undertow.util.Headers;
import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.CancelHandle;
import org.wildfly.httpclient.common.HttpServiceConfig;
import org.wildfly.transaction.client.LocalTransactionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * HTTP service that handles EJB calls.
 *
 * @author Stuart Douglas
 * @author Flavia Rainone
 */
public class EjbHttpService {

    private final Association association;
    private final ExecutorService executorService;
    private final LocalTransactionContext localTransactionContext;
    private final Function<String, Boolean> classResolverFilter;
    private final HttpServiceConfig httpServiceConfig;

    private final Map<InvocationIdentifier, CancelHandle> cancellationFlags = new ConcurrentHashMap<>();

    @Deprecated
    public EjbHttpService(Association association, ExecutorService executorService, LocalTransactionContext localTransactionContext) {
        this(HttpServiceConfig.getInstance(), association, executorService, localTransactionContext, null);
    }

    public EjbHttpService(Association association, ExecutorService executorService, LocalTransactionContext localTransactionContext,
                          Function<String, Boolean> classResolverFilter) {
        this(HttpServiceConfig.getInstance(), association, executorService, localTransactionContext, classResolverFilter);
    }

    public EjbHttpService(HttpServiceConfig httpServiceConfig, Association association, ExecutorService executorService, LocalTransactionContext localTransactionContext,
                          Function<String, Boolean> classResolverFilter) {
        this.httpServiceConfig = httpServiceConfig;
        this.association = association;
        this.executorService = executorService;
        this.localTransactionContext = localTransactionContext;
        this.classResolverFilter = classResolverFilter;
    }

    public HttpHandler createHttpHandler() {
        PathHandler pathHandler = new PathHandler();
        registerHandler(pathHandler, CANCEL);
        registerHandler(pathHandler, CREATE_SESSION);
        registerHandler(pathHandler, DISCOVER);
        registerHandler(pathHandler, INVOKE);

        EncodingHandler encodingHandler = new EncodingHandler(pathHandler, new ContentEncodingRepository().addEncodingHandler(Headers.GZIP.toString(), new GzipEncodingProvider(), 1));
        RequestEncodingHandler requestEncodingHandler = new RequestEncodingHandler(encodingHandler);
        requestEncodingHandler.addEncoding(Headers.GZIP.toString(), GzipStreamSourceConduit.WRAPPER);
        return httpServiceConfig.wrap(requestEncodingHandler);
    }

    private void registerHandler(final PathHandler pathHandler, final RequestType requestType) {
        pathHandler.addPrefixPath(requestType.getPath(), new AllowedMethodsHandler(newInvocationHandler(requestType), requestType.getMethod()));
    }

    private HttpHandler newInvocationHandler(final RequestType requestType) {
        if (requestType == CANCEL) return new HttpCancelHandler(association, executorService, localTransactionContext, cancellationFlags);
        if (requestType == CREATE_SESSION) return new HttpSessionOpenHandler(association, executorService, localTransactionContext, httpServiceConfig);
        if (requestType == DISCOVER) return new HttpDiscoveryHandler(executorService, association, httpServiceConfig);
        if (requestType == INVOKE) return new HttpInvocationHandler(association, executorService, localTransactionContext, cancellationFlags, classResolverFilter, httpServiceConfig);
        throw new IllegalStateException();
    }

}
