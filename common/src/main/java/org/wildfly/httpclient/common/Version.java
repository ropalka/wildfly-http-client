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

public final class Version {

    protected static final Version LATEST = new Version(false, Handler.VERSION_2, Specification.JAKARTA_EE_9, Encoding.JBOSS_MARSHALLING);
    protected static final Version LEGACY = new Version(true, Handler.VERSION_1, Specification.JAVA_EE_8, Encoding.JBOSS_MARSHALLING);

    private final boolean requiresTransformation;
    private final Handler handlerVersion;
    private final Specification specificationVersion;
    private final Encoding encodingVersion;

    private Version(final boolean requiresTransformation, final Handler handlerVersion, final Specification specificationVersion, final Encoding encodingVersion) {
        this.requiresTransformation = requiresTransformation;
        this.handlerVersion = handlerVersion;
        this.specificationVersion = specificationVersion;
        this.encodingVersion = encodingVersion;
    }

    public boolean requiresTransformation() {
        return requiresTransformation;
    }

    public Handler handler() {
        return handlerVersion;
    }

    public Specification specitication() {
        return specificationVersion;
    }

    public Encoding encoding() {
        return encodingVersion;
    }

    public enum Handler {
        VERSION_1(0),
        VERSION_2(1);

        private final int value;

        private Handler(final int value) {
            this.value = value;
        }
    }

    public enum Specification {
        JAVA_EE_8(-1),
        JAKARTA_EE_9(0),
        JAKARTA_EE_9_1(1),
        JAKARTA_EE_10(2),
        JAKARTA_EE_11(3);

        private final int value;

        private Specification(final int value) {
            this.value = value;
        }
    }

    public enum Encoding {
        JBOSS_MARSHALLING(0);

        private final int value;

        private Encoding(final int value) {
            this.value = value;
        }
    }

}
