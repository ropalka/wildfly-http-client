/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025 Red Hat, Inc., and individual contributors
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

import static org.wildfly.httpclient.common.HeadersHelper.getRequestHeader;
import static java.lang.Integer.decode;

import java.util.Objects;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public final class Version {

    private static final HttpString PROTOCOL_VERSION = new HttpString("x-wf-version");
    private static final int MASK_INTEROP  = 0b00000000_00000000_00000000_00000001;
    private static final int MASK_HANDLER  = 0b00000000_00000000_00000000_01111110;
    private static final int MASK_SPEC     = 0b00000000_00000000_00011111_10000000;
    private static final int MASK_ENCODING = 0b00000000_00000000_11100000_00000000;

    protected static final Version JAVA_EE_8 = new Version(true, Handler.VERSION_1, Specification.JAVA_EE_8, Encoding.JBOSS_MARSHALLING);
    protected static final Version JAKARTA_EE_9 = new Version(false, Handler.VERSION_2, Specification.JAKARTA_EE_9, Encoding.JBOSS_MARSHALLING);
    protected static final Version JAKARTA_EE_10 = new Version(false, Handler.VERSION_2, Specification.JAKARTA_EE_10, Encoding.JBOSS_MARSHALLING);
    protected static final Version LATEST = JAKARTA_EE_10;

    private final Boolean requiresTransformation;
    private final Handler handlerVersion;
    private final Specification specVersion;
    private final Encoding encodingVersion;

    private Version(final boolean requiresTransformation, final Handler handlerVersion, final Specification specVersion, final Encoding encodingVersion) {
        this.requiresTransformation = requiresTransformation;
        this.handlerVersion = handlerVersion;
        this.specVersion = specVersion;
        this.encodingVersion = encodingVersion;
    }

    private static Version of(final int version) {
        if (JAVA_EE_8.equals(version)) return JAVA_EE_8;
        if (JAKARTA_EE_9.equals(version)) return JAKARTA_EE_9;
        if (JAKARTA_EE_10.equals(version)) return JAKARTA_EE_10;
        final Boolean requiresTransformation = (version & MASK_INTEROP) > 0;
        final Handler handlerVersion = Handler.of((version & MASK_HANDLER) >>> 1);
        final Specification specVersion = Specification.of((version & MASK_SPEC) >>> 7);
        final Encoding encodingVersion = Encoding.of((version & MASK_ENCODING) >>> 13);
        return new Version(requiresTransformation, handlerVersion, specVersion, encodingVersion);
    }

    public boolean requiresTransformation() {
        return requiresTransformation;
    }

    public Handler handler() {
        return handlerVersion;
    }

    public Specification specitication() {
        return specVersion;
    }

    public Encoding encoding() {
        return encodingVersion;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Version)) return false;
        final Version v = (Version)o;
        return requiresTransformation.equals(v.requiresTransformation) &&
               handlerVersion.equals(v.handlerVersion) &&
               specVersion.equals(v.specVersion) &&
               encodingVersion.equals(v.encodingVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requiresTransformation, handlerVersion, specVersion, encodingVersion);
    }

    public static Version readFrom(final HttpServerExchange exchange) {
        final String versionHeader = getRequestHeader(exchange, PROTOCOL_VERSION);
        final Version version = versionHeader == null ? JAVA_EE_8 : Version.of(decode(versionHeader));
        if (version == JAVA_EE_8) {
            // transformation is required for unmarshalling request and marshalling response,
            // because server is interoperable mode and the lack of a header indicates this is
            // either a Javax EE client or a Jakarta EE client that is not interoperable
            // the latter case will lead to an error when unmarshalling at client side)
            exchange.putAttachment(HTTP_MARSHALLER_FACTORY_KEY, INTEROPERABLE_MARSHALLER_FACTORY);
            exchange.putAttachment(HTTP_UNMARSHALLER_FACTORY_KEY, INTEROPERABLE_MARSHALLER_FACTORY);
        } else {

        }
        return version;
    }

    public void writeTo(final HttpServerExchange exchange) {
        if (this.equals(JAVA_EE_8)) return;

    }

    public enum Handler {
        VERSION_1(0),
        VERSION_2(1);

        private final int value;

        private Handler(final int value) {
            this.value = value;
        }

        private static Handler of(final int value) {
            for (Handler handler : values()) {
                if (value == handler.value) return handler;
            }
            throw new IllegalArgumentException("Unsupported Handler Version");
        }
    }

    public enum Specification {
        JAVA_EE_8(-1),
        JAKARTA_EE_9(0),
        JAKARTA_EE_10(1);

        private final int value;

        private Specification(final int value) {
            this.value = value;
        }

        private static Specification of(final int value) {
            for (Specification spec : values()) {
                if (value == spec.value) return spec;
            }
            throw new IllegalArgumentException("Unsupported Specification Version");
        }
    }

    public enum Encoding {
        JBOSS_MARSHALLING(0);

        private final int value;

        private Encoding(final int value) {
            this.value = value;
        }

        private static Encoding of(final int value) {
            for (Encoding encoding : values()) {
                if (value == encoding.value) return encoding;
            }
            throw new IllegalArgumentException("Unsupported Encoding Version");
        }
    }

}
