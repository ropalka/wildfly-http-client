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
package org.wildfly.httpclient.naming;

import static org.wildfly.httpclient.naming.ByteOutputs.byteOutputOf;
import static org.wildfly.httpclient.naming.Serializer.deserializeObject;
import static org.wildfly.httpclient.naming.Serializer.serializeObject;

import io.undertow.client.ClientResponse;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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

    static HttpTargetContext.HttpMarshaller objectRequestHandler(final Marshaller marshaller, final Object object) {
        return new ObjectRequestHandler(marshaller, object);
    }

    static HttpTargetContext.HttpResultHandler objectResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Object> result) {
        return new ObjectResponseHandler(unmarshaller, result);
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

    private static final class ObjectRequestHandler implements HttpTargetContext.HttpMarshaller {
        private final Marshaller marshaller;
        private final Object object;

        private ObjectRequestHandler(final Marshaller marshaller, final Object object) {
            this.marshaller = marshaller;
            this.object = object;
        }

        @Override
        public void marshall(final OutputStream httpBodyRequestStream) throws Exception {
            try (ByteOutput out = byteOutputOf(httpBodyRequestStream)) {
                marshaller.start(out);
                serializeObject(marshaller, object);
                marshaller.finish();
            }
        }
    }

    private static final class ObjectResponseHandler implements HttpTargetContext.HttpResultHandler {
        private final CompletableFuture<Object> result;
        private final Unmarshaller unmarshaller;

        private ObjectResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Object> result) {
            this.unmarshaller = unmarshaller;
            this.result = result;
        }

        @Override
        public void handleResult(final InputStream httpBodyResponseStream, final ClientResponse httpResponse, final Closeable doneCallback) {
            try (ByteInput in = new InputStreamByteInput(httpBodyResponseStream)) {
                unmarshaller.start(in);
                Object object = deserializeObject(unmarshaller);
                unmarshaller.finish();
                result.complete(object);
            } catch (Exception e) {
                result.completeExceptionally(e);
            } finally {
                IoUtils.safeClose(doneCallback);
            }
        }
    }

}
