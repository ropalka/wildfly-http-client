/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024 Red Hat, Inc., and individual contributors
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

import io.undertow.client.ClientResponse;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.ejb.client.SessionID;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.httpclient.common.HttpTargetContext;
import org.xnio.IoUtils;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.wildfly.httpclient.ejb.ByteOutputs.byteOutputOf;
import static org.wildfly.httpclient.ejb.Serializer.deserializeSet;
import static org.wildfly.httpclient.ejb.Serializer.serializeTransaction;
import static org.xnio.IoUtils.safeClose;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ClientHandlers {

    private ClientHandlers() {
        // forbidden instantiation
    }

    static HttpTargetContext.HttpResultHandler discoveryResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Set<EJBModuleIdentifier>> result) {
        return new DiscoveryResponseHandler(unmarshaller, result);
    }

    static <T> HttpTargetContext.HttpResultHandler emptyResponseHandler(final CompletableFuture<T> result, final Function<ClientResponse, T> function) {
        return new EmptyResponseHandler<T>(result, function);
    }

    static HttpTargetContext.HttpMarshaller transactionRequestHandler(final Marshaller marshaller, final TransactionInfo txnInfo) {
        return new TransactionRequestHandler(marshaller, txnInfo);
    }

    static Function<ClientResponse, Boolean> cancelInvocationResponseFunction() {
        return new CancelInvocationResponseFunction();
    }

    static Function<ClientResponse, SessionID> ejbSessionIdResponseFunction() {
        return new EjbSessionIdResponseFunction();
    }

    private static final class CancelInvocationResponseFunction implements Function<ClientResponse, Boolean> {
        @Override
        public Boolean apply(final ClientResponse clientResponse) {
            return true;
        }
    }

    private static final class EjbSessionIdResponseFunction implements Function<ClientResponse, SessionID> {
        @Override
        public SessionID apply(final ClientResponse clientResponse) {
            final String sessionId = clientResponse.getResponseHeaders().getFirst(Constants.EJB_SESSION_ID);
            if (sessionId != null) {
                return SessionID.createSessionID(Base64.getUrlDecoder().decode(sessionId));
            }
            throw new IllegalStateException(EjbHttpClientMessages.MESSAGES.noSessionIdInResponse());
        }
    }

    private static final class DiscoveryResponseHandler implements HttpTargetContext.HttpResultHandler {
        private final CompletableFuture<Set<EJBModuleIdentifier>> result;
        private final Unmarshaller unmarshaller;

        private DiscoveryResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Set<EJBModuleIdentifier>> result) {
            this.unmarshaller = unmarshaller;
            this.result = result;
        }

        @Override
        public void handleResult(final InputStream httpBodyResponseStream, final ClientResponse httpResponse, final Closeable doneCallback) {
            try (ByteInput in = new InputStreamByteInput(httpBodyResponseStream)) {
                Set<EJBModuleIdentifier> modules;
                unmarshaller.start(in);
                modules = deserializeSet(unmarshaller);
                unmarshaller.finish();
                result.complete(modules);
            } catch (Exception e) {
                result.completeExceptionally(e);
            } finally {
                safeClose(doneCallback);
            }
        }
    }

    private static final class EmptyResponseHandler<T> implements HttpTargetContext.HttpResultHandler {
        private final CompletableFuture<T> result;
        private final Function<ClientResponse, T> function;

        private EmptyResponseHandler(final CompletableFuture<T> result, final Function<ClientResponse, T> function) {
            this.result = result;
            this.function = function;
        }

        @Override
        public void handleResult(final InputStream httpBodyResponseStream, final ClientResponse httpResponse, final Closeable doneCallback) {
            try {
                result.complete(function != null ? function.apply(httpResponse) : null);
            } finally {
                IoUtils.safeClose(doneCallback);
            }
        }
    }

    private static final class TransactionRequestHandler implements HttpTargetContext.HttpMarshaller {
        private final Marshaller marshaller;
        private final TransactionInfo txnInfo;

        private TransactionRequestHandler(final Marshaller marshaller, final TransactionInfo txnInfo) {
            this.marshaller = marshaller;
            this.txnInfo = txnInfo;
        }

        @Override
        public void marshall(final OutputStream httpBodyRequestStream) throws Exception {
            try (ByteOutput out = byteOutputOf(httpBodyRequestStream)) {
                marshaller.start(out);
                serializeTransaction(marshaller, txnInfo);
                marshaller.finish();
            }
        }
    }
}
