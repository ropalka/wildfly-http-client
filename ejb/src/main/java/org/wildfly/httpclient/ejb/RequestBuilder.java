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

package org.wildfly.httpclient.ejb;

import static io.undertow.util.Headers.ACCEPT;
import static io.undertow.util.Headers.CONTENT_TYPE;
import static io.undertow.util.Methods.DELETE;
import static io.undertow.util.Methods.GET;
import static io.undertow.util.Methods.POST;

import static org.wildfly.httpclient.ejb.EjbConstants.EJB_CANCEL_PATH;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_DISCOVER_PATH;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_DISCOVERY_RESPONSE;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_EXCEPTION;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_INVOKE_PATH;
import static org.wildfly.httpclient.ejb.EjbConstants.EJB_OPEN_PATH;
import static org.wildfly.httpclient.ejb.EjbConstants.INVOCATION_ACCEPT;
import static org.wildfly.httpclient.ejb.EjbConstants.INVOCATION_ID;
import static org.wildfly.httpclient.ejb.EjbConstants.INVOCATION;
import static org.wildfly.httpclient.ejb.EjbConstants.SESSION_OPEN;
import static org.wildfly.httpclient.ejb.RequestType.CANCEL_EJB_INVOCATION;
import static org.wildfly.httpclient.ejb.RequestType.CREATE_SESSION_EJB;
import static org.wildfly.httpclient.ejb.RequestType.DISCOVER_EJB;
import static org.wildfly.httpclient.ejb.RequestType.START_EJB_INVOCATION;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.net.URLEncoder.encode;

import io.undertow.client.ClientRequest;
import io.undertow.util.HeaderMap;
import org.jboss.ejb.client.EJBLocator;
import org.wildfly.httpclient.common.Protocol;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class RequestBuilder {

    private EJBLocator<?> locator;
    private String beanId;
    private String view;
    private Method method;
    private RequestType requestType;
    private String invocationId;
    private int version = Protocol.LATEST;
    private boolean cancelIfRunning;

    // setters

    RequestBuilder setLocator(final EJBLocator<?> locator) {
        this.locator = locator;
        return this;
    }

    RequestBuilder setBeanId(final String beanId) {
        this.beanId = beanId;
        return this;
    }

    RequestBuilder setMethod(final Method method) {
        this.method = method;
        return this;
    }

    RequestBuilder setView(final String view) {
        this.view = view;
        return this;
    }

    RequestBuilder setRequestType(final RequestType requestType) {
        this.requestType = requestType;
        return this;
    }

    RequestBuilder setInvocationId(final String invocationId) {
        this.invocationId = invocationId;
        return this;
    }

    RequestBuilder setVersion(final int version) {
        this.version = version;
        return this;
    }

    RequestBuilder setCancelIfRunning(final boolean cancelIfRunning) {
        this.cancelIfRunning = cancelIfRunning;
        return this;
    }

    // helper methods

    ClientRequest createRequest(final String prefix) {
        final ClientRequest clientRequest = new ClientRequest();
        setRequestMethod(clientRequest);
        setRequestPath(clientRequest, prefix);
        setRequestHeaders(clientRequest);
        return clientRequest;
    }

    private void setRequestMethod(final ClientRequest request) {
        if (requestType == START_EJB_INVOCATION) request.setMethod(POST);
        else if (requestType == CREATE_SESSION_EJB) request.setMethod(POST);
        else if (requestType == DISCOVER_EJB) request.setMethod(GET);
        else if (requestType == CANCEL_EJB_INVOCATION) request.setMethod(DELETE);
        else throw new IllegalStateException();
    }

    private void setRequestPath(final ClientRequest request, final String prefix) {
        if (requestType == START_EJB_INVOCATION) request.setPath(getStartEjbInvocationRequestPath(prefix));
        else if (requestType == CREATE_SESSION_EJB) request.setPath(getCreateSessionEjbRequestPath(prefix));
        else if (requestType == DISCOVER_EJB) request.setPath(getDiscoverEjbRequestPath(prefix));
        else if (requestType == CANCEL_EJB_INVOCATION) request.setPath(getCancelEjbInvocationRequestPath(prefix));
        else throw new IllegalStateException();
    }

    private void setRequestHeaders(final ClientRequest request) {
        final HeaderMap headers = request.getRequestHeaders();
        if (requestType == START_EJB_INVOCATION) {
            headers.add(ACCEPT, INVOCATION_ACCEPT + "," + EJB_EXCEPTION);
            headers.put(CONTENT_TYPE, INVOCATION.toString());
            if (invocationId != null) {
                headers.put(INVOCATION_ID, invocationId);
            }
        } else if (requestType == CREATE_SESSION_EJB) {
            headers.add(ACCEPT, EJB_EXCEPTION.toString());
            headers.put(CONTENT_TYPE, SESSION_OPEN.toString());
        } else if (requestType == DISCOVER_EJB) {
            headers.add(ACCEPT, EJB_DISCOVERY_RESPONSE + "," + EJB_EXCEPTION);
        } else if (requestType != CANCEL_EJB_INVOCATION) {
            throw new IllegalStateException();
        }
    }

    private String getCreateSessionEjbRequestPath(final String prefix) {
        final StringBuilder sb = new StringBuilder();
        appendOperationPath(sb, prefix, EJB_OPEN_PATH);
        appendBeanPath(sb);
        return sb.toString();
    }

    private String getDiscoverEjbRequestPath(final String prefix) {
        final StringBuilder sb = new StringBuilder();
        appendOperationPath(sb, prefix, EJB_DISCOVER_PATH);
        return sb.toString();
    }

    private String getCancelEjbInvocationRequestPath(final String prefix) {
        final StringBuilder sb = new StringBuilder();
        appendOperationPath(sb, prefix, EJB_CANCEL_PATH);
        appendBeanPath(sb);
        appendPath(sb, invocationId, false);
        appendPath(sb, "" + cancelIfRunning, false);
        return sb.toString();
    }

    private String getStartEjbInvocationRequestPath(final String prefix) {
        final StringBuilder sb = new StringBuilder();
        appendOperationPath(sb, prefix, EJB_INVOKE_PATH);
        appendBeanPath(sb);
        appendPath(sb, beanId, false);
        appendPath(sb, view, false);
        appendPath(sb, method.getName(), false);
        for (final Class<?> param : method.getParameterTypes()) {
            appendPath(sb, param.getName(), true);
        }
        return sb.toString();
    }

    private void appendBeanPath(final StringBuilder sb) {
        appendPath(sb, locator.getAppName(), true);
        appendPath(sb, locator.getModuleName(), true);
        appendPath(sb, locator.getDistinctName(), true);
        appendPath(sb, locator.getBeanName(), true);
    }

    private void appendOperationPath(final StringBuilder sb, final String prefix, final String operationType) {
        if (prefix != null) {
            sb.append(prefix);
        }
        appendPath(sb, "ejb", false);
        appendPath(sb, "v" + version, false);
        appendPath(sb, operationType, false);
    }

    private static void appendPath(final StringBuilder sb, final String path, final boolean encode) {
        sb.append("/").append(path == null || path.isEmpty() ? "-" : encode ? encode(path, UTF_8) : path);
    }

}
