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
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.wildfly.httpclient.common.HttpTargetContext;
import org.xnio.IoUtils;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.wildfly.httpclient.ejb.ByteOutputs.byteOutputOf;
import static org.wildfly.httpclient.ejb.Serializer.serializeTransaction;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ClientHandlers {

    private ClientHandlers() {
        // forbidden instantiation
    }

    static <T> HttpTargetContext.HttpResultHandler emptyResponseHandler(final CompletableFuture<T> result, final Function<ClientResponse, T> function) {
        return new EmptyResponseHandler<T>(result, function);
    }

    static HttpTargetContext.HttpMarshaller transactionRequestHandler(final Marshaller marshaller, final TransactionInfo txnInfo) {
        return new TransactionRequestHandler(marshaller, txnInfo);
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
