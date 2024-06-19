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
package org.wildfly.httpclient.transaction;

import static org.wildfly.httpclient.transaction.ByteOutputs.byteOutputOf;
import static org.wildfly.httpclient.transaction.Serializer.deserializeXid;
import static org.wildfly.httpclient.transaction.Serializer.deserializeXidArray;
import static org.wildfly.httpclient.transaction.Serializer.serializeXid;

import io.undertow.client.ClientResponse;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.httpclient.common.HttpTargetContext;
import org.wildfly.httpclient.common.NoFlushByteOutput;
import org.xnio.IoUtils;

import javax.transaction.xa.Xid;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class Helper {

    private Helper() {
        // forbidden instantiation
    }

    static <T> HttpTargetContext.HttpResultHandler emptyResponseHandler(final CompletableFuture<T> result, final Function<ClientResponse, T> function) {
        return new EmptyResponseHandler<T>(result, function);
    }

    static HttpTargetContext.HttpMarshaller xidRequestHandler(final Marshaller marshaller, final Xid xid) {
        return new XidRequestHandler(marshaller, xid);
    }

    static HttpTargetContext.HttpResultHandler xidResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Xid> result) {
        return new XidResponseHandler(unmarshaller, result);
    }

    static HttpTargetContext.HttpResultHandler xidArrayResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Xid[]> result) {
        return new XidArrayResponseHandler(unmarshaller, result);
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

    private static final class XidRequestHandler implements HttpTargetContext.HttpMarshaller {
        private final Marshaller marshaller;
        private final Xid xid;

        private XidRequestHandler(final Marshaller marshaller, final Xid xid) {
            this.marshaller = marshaller;
            this.xid = xid;
        }

        @Override
        public void marshall(final OutputStream httpBodyRequestStream) throws Exception {
            try (ByteOutput out = new NoFlushByteOutput(byteOutputOf(httpBodyRequestStream))) {
                marshaller.start(out);
                serializeXid(marshaller, xid);
                marshaller.finish();
            }
        }
    }

    private static final class XidResponseHandler implements HttpTargetContext.HttpResultHandler {
        private final CompletableFuture<Xid> result;
        private final Unmarshaller unmarshaller;

        private XidResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Xid> result) {
            this.unmarshaller = unmarshaller;
            this.result = result;
        }

        @Override
        public void handleResult(final InputStream httpBodyResponseStream, final ClientResponse httpResponse, final Closeable doneCallback) {
            try (ByteInput in = new InputStreamByteInput(httpBodyResponseStream)) {
                unmarshaller.start(in);
                Xid xid = deserializeXid(unmarshaller);
                unmarshaller.finish();
                result.complete(xid);
            } catch (Exception e) {
                result.completeExceptionally(e);
            } finally {
                IoUtils.safeClose(doneCallback);
            }
        }
    }

    private static final class XidArrayResponseHandler implements HttpTargetContext.HttpResultHandler {
        private final CompletableFuture<Xid[]> result;
        private final Unmarshaller unmarshaller;

        private XidArrayResponseHandler(final Unmarshaller unmarshaller, final CompletableFuture<Xid[]> result) {
            this.unmarshaller = unmarshaller;
            this.result = result;
        }

        @Override
        public void handleResult(final InputStream httpBodyResponseStream, final ClientResponse httpResponse, final Closeable doneCallback) {
            try (ByteInput in = new InputStreamByteInput(httpBodyResponseStream)) {
                unmarshaller.start(in);
                Xid[] ret = deserializeXidArray(unmarshaller);
                unmarshaller.finish();
                result.complete(ret);
            } catch (Exception e) {
                result.completeExceptionally(e);
            } finally {
                IoUtils.safeClose(doneCallback);
            }
        }
    }

}
