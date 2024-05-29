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

import static io.undertow.util.Methods.GET;
import static io.undertow.util.Methods.POST;

import io.undertow.util.HttpString;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
enum RequestType {

    UT_BEGIN(POST, "/ut/begin"),
    UT_COMMIT(POST, "/ut/commit"),
    UT_ROLLBACK(POST, "/ut/rollback"),
    XA_BEFORE_COMPLETION(POST, "/xa/bc"),
    XA_COMMIT(POST, "/xa/commit"),
    XA_FORGET(POST, "/xa/forget"),
    XA_PREPARE(POST, "/xa/prep"),
    XA_RECOVER(GET, "/xa/recover"),
    XA_ROLLBACK(POST, "/xa/rollback");

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
