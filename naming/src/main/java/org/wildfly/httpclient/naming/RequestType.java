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

import io.undertow.util.HttpString;

import static io.undertow.util.Methods.DELETE;
import static io.undertow.util.Methods.GET;
import static io.undertow.util.Methods.PATCH;
import static io.undertow.util.Methods.POST;
import static io.undertow.util.Methods.PUT;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
enum RequestType {

    BIND(PUT, "/bind"),
    CREATE_SUBCONTEXT(PUT, "/create-subcontext"),
    DESTROY_SUBCONTEXT(DELETE, "/dest-subctx"),
    LIST(GET, "/list"),
    LIST_BINDINGS(GET, "/list-bindings"),
    LOOKUP(POST, "/lookup"),
    LOOKUP_LINK(POST, "/lookuplink"),
    REBIND(PATCH, "/rebind"),
    RENAME(PATCH, "/rename"),
    UNBIND(DELETE, "/unbind");

    private final HttpString method;
    private final String path;

    RequestType(final HttpString method, final String path) {
        this.method = method;
        this.path = path;
    }

    HttpString getMethod() {
        return method;
    }

    String getPath() {
        return path;
    }

}
